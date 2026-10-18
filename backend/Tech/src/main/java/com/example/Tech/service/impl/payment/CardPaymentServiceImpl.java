package com.example.Tech.service.impl.payment;

import com.example.Tech.config.CorsProperties;
import com.example.Tech.config.StripeProperties;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.dto.response.payment.CardCheckoutResponse;
import com.example.Tech.dto.response.payment.CardOptionsResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import com.example.Tech.service.impl.order.OrderViewLoader;
import com.example.Tech.service.payment.CardPaymentService;
import com.example.Tech.service.payment.StripeCheckoutSession;
import com.example.Tech.service.payment.StripeGateway;
import com.example.Tech.service.payment.StripeSignature;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.cors.CorsConfiguration;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
public class CardPaymentServiceImpl implements CardPaymentService {

    private static final Set<String> PAID_EVENTS =
            Set.of("checkout.session.completed", "checkout.session.async_payment_succeeded");

    private final StripeProperties stripe;
    private final StripeGateway gateway;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderViewLoader orderViewLoader;
    private final CurrentUserLoader currentUserLoader;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    /** Stripe may only send the customer back to a page of this shop: an origin the browser may call the API from. */
    private final CorsConfiguration returnOrigins;

    public CardPaymentServiceImpl(StripeProperties stripe, StripeGateway gateway, OrderRepository orderRepository,
                                  PaymentRepository paymentRepository, OrderViewLoader orderViewLoader,
                                  CurrentUserLoader currentUserLoader, JsonMapper jsonMapper, Clock clock,
                                  CorsProperties cors) {
        this.stripe = stripe;
        this.gateway = gateway;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderViewLoader = orderViewLoader;
        this.currentUserLoader = currentUserLoader;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.returnOrigins = new CorsConfiguration();
        this.returnOrigins.setAllowedOrigins(cors.allowedOrigins());
        this.returnOrigins.setAllowedOriginPatterns(cors.allowedOriginPatterns());
    }

    @Override
    public CardOptionsResponse options() {
        return new CardOptionsResponse(stripe.enabled(), stripe.testMode(), BigDecimal.valueOf(StripeProperties.MAX_AMOUNT_VND));
    }

    @Override
    @Transactional
    public CardCheckoutResponse startCheckout(Long userId, Long orderId, String returnUrl) {
        if (!stripe.enabled()) {
            throw new BusinessException(ErrorCode.CARD_PAYMENT_DISABLED);
        }
        String page = checkedReturnUrl(returnUrl);
        currentUserLoader.load(userId);
        // locked: a double click opens one Stripe page, not two that could both be paid
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(found -> found.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
        Payment payment = awaitedCardPayment(order);

        if (payment.getCheckoutSessionId() != null) {
            StripeCheckoutSession current = gateway.getSession(payment.getCheckoutSessionId());
            if (current.paid()) {
                recordPaid(order, payment, current);
                return new CardCheckoutResponse(null, true);
            }
            if (current.open() && current.url() != null) {
                return new CardCheckoutResponse(current.url(), false);
            }
        }
        String back = page + "?id=" + order.getId() + "&card=";
        StripeCheckoutSession created = gateway.createSession(new StripeGateway.NewSession(order.getId(),
                OrderMapper.code(order.getId()), amountVnd(payment.getAmount()), order.getUser().getEmail(),
                back + "paid", back + "back", Instant.now(clock).plusSeconds(stripe.sessionMinutes() * 60L)));
        payment.setCheckoutSessionId(created.id());
        log.info("Stripe session {} opened for order id={} ({} VND)", created.id(), order.getId(), payment.getAmount());
        return new CardCheckoutResponse(created.url(), false);
    }

    @Override
    @Transactional
    public OrderResponse refresh(Long userId, Long orderId) {
        currentUserLoader.load(userId);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
        String sessionId = paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(orderId)
                .filter(payment -> payment.getPaymentMethod() == PaymentMethod.CARD)
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .map(Payment::getCheckoutSessionId)
                .orElse(null);
        if (sessionId != null && stripe.enabled()) {
            StripeCheckoutSession session = gateway.getSession(sessionId);
            if (session.paid()) {
                Order locked = orderRepository.findByIdForUpdate(orderId).orElseThrow();
                paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(orderId)
                        .ifPresent(payment -> recordPaid(locked, payment, session));
                order = locked;
            }
        }
        return orderViewLoader.toResponse(order);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (!stripe.webhookConfigured()) {
            throw new BusinessException(ErrorCode.WEBHOOK_NOT_CONFIGURED, "Webhook Stripe chưa được cấu hình");
        }
        if (!StripeSignature.isValid(payload, signatureHeader, stripe.webhookSecret(), Instant.now(clock).getEpochSecond())) {
            throw new BusinessException(ErrorCode.STRIPE_SIGNATURE_INVALID);
        }
        JsonNode event;
        try {
            event = jsonMapper.readTree(payload);
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.STRIPE_SIGNATURE_INVALID, "Nội dung webhook Stripe không đọc được");
        }
        String type = event.path("type").asString("");
        if (!PAID_EVENTS.contains(type)) {
            log.debug("Stripe event {} ignored", type);
            return;
        }
        StripeCheckoutSession session = StripeHttpGateway.toSession(event.path("data").path("object"));
        if (!session.paid() || session.orderId() == null) {
            return;
        }
        orderRepository.findByIdForUpdate(session.orderId()).ifPresentOrElse(
                order -> paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                        .ifPresent(payment -> recordPaid(order, payment, session)),
                () -> log.warn("Stripe session {} paid for unknown order id={}", session.id(), session.orderId()));
    }

    /**
     * Stripe has the money of this session. Awaited → PAID; the order was cancelled meanwhile → REFUND_PENDING (staff
     * refunds in the Stripe dashboard). Another amount / currency is never taken as paid. Runs with the order locked.
     */
    private void recordPaid(Order order, Payment payment, StripeCheckoutSession session) {
        if (payment.getPaymentMethod() != PaymentMethod.CARD) {
            return;
        }
        if (session.amountTotal() == null || session.amountTotal() != amountVnd(payment.getAmount())
                || !"vnd".equalsIgnoreCase(session.currency())) {
            log.warn("Stripe session {} of order id={} paid {} {}, expected {} VND: not recorded", session.id(),
                    order.getId(), session.amountTotal(), session.currency(), payment.getAmount());
            return;
        }
        String reference = session.paymentIntent() != null ? session.paymentIntent() : session.id();
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now(clock));
            payment.setTransactionId(reference);
            payment.setCheckoutSessionId(session.id());
            log.info("Card payment of order id={} received (Stripe {})", order.getId(), reference);
        } else if (payment.getStatus() == PaymentStatus.CANCELLED) {
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            payment.setPaidAt(LocalDateTime.now(clock));
            payment.setTransactionId(reference);
            log.warn("Order id={} was cancelled but Stripe {} was paid: waiting for a refund", order.getId(), reference);
        }
    }

    private Payment awaitedCardPayment(Order order) {
        String code = OrderMapper.code(order.getId());
        Payment payment = paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                .filter(found -> found.getPaymentMethod() == PaymentMethod.CARD)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
                        "Đơn %s không thanh toán bằng thẻ".formatted(code)));
        if (payment.getStatus() != PaymentStatus.PENDING || !order.getStatus().canBeCancelledByCustomer()) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
                    "Đơn %s không còn chờ thanh toán thẻ".formatted(code));
        }
        return payment;
    }

    /** An http(s) page of an allowed origin ending in /payment.html, without query or fragment (the shop adds its own). */
    private String checkedReturnUrl(String returnUrl) {
        try {
            URI uri = new URI(returnUrl.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            boolean page = (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null && uri.getRawUserInfo() == null
                    && uri.getRawPath() != null && uri.getRawPath().endsWith("/payment.html");
            if (page) {
                String origin = scheme + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
                if (returnOrigins.checkOrigin(origin) != null) {
                    return origin + uri.getRawPath();
                }
            }
        } catch (URISyntaxException e) {
            // answered below
        }
        throw BusinessException.invalidField("returnUrl", "Địa chỉ quay về không thuộc trang web của cửa hàng");
    }

    private static long amountVnd(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.CEILING).longValueExact();
    }
}
