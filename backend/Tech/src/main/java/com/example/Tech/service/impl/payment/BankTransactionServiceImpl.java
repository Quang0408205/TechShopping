package com.example.Tech.service.impl.payment;

import com.example.Tech.config.BankTransferProperties;
import com.example.Tech.config.PaymentWebhookProperties;
import com.example.Tech.dto.request.payment.BankSimulateRequest;
import com.example.Tech.dto.request.payment.BankTransactionAssignRequest;
import com.example.Tech.dto.request.payment.BankTransactionIgnoreRequest;
import com.example.Tech.dto.request.payment.BankWebhookRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.payment.BankReconciliationInfoResponse;
import com.example.Tech.dto.response.payment.BankTransactionResponse;
import com.example.Tech.dto.response.payment.BankWebhookResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.BankTransaction;
import com.example.Tech.entity.payment.BankTransactionStatus;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.payment.BankTransactionRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import com.example.Tech.service.payment.BankTransactionService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankTransactionServiceImpl implements BankTransactionService {

    /** The order code is DH + 8 digits (OrderMapper.code); banks may change the case or add a space. */
    private static final Pattern ORDER_CODE = Pattern.compile("(?i)DH\\s*(\\d{8})");
    private static final DateTimeFormatter BANK_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final EnumSet<BankTransactionStatus> ATTENTION =
            EnumSet.of(BankTransactionStatus.UNMATCHED, BankTransactionStatus.REVIEW);
    private static final int MAX_CONTENT = 500;

    private final BankTransactionRepository bankTransactionRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderPaymentLifecycle orderPaymentLifecycle;
    private final CurrentUserLoader currentUserLoader;
    private final BankTransferProperties bankTransfer;
    private final PaymentWebhookProperties webhook;
    private final Clock clock;

    @Override
    @Transactional
    public BankWebhookResponse receive(BankWebhookRequest request, String provider) {
        if (!"in".equalsIgnoreCase(request.transferType().trim())) {
            return new BankWebhookResponse(false, null, null);
        }
        String externalId = request.id().trim();
        if (bankTransactionRepository.existsByExternalId(externalId)) {
            return new BankWebhookResponse(true, null, null);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        BankTransaction tx = new BankTransaction();
        tx.setExternalId(externalId);
        tx.setProvider(provider);
        tx.setAmount(request.transferAmount());
        tx.setContent(truncate(request.content(), MAX_CONTENT));
        tx.setAccountNumber(request.accountNumber() == null ? null : request.accountNumber().trim());
        tx.setTransferredAt(parseTime(request.transactionDate()));
        tx.setReceivedAt(now);

        Long orderId = orderIdFrom(request.content());
        if (tx.getAccountNumber() != null && !tx.getAccountNumber().isEmpty()
                && !tx.getAccountNumber().equals(bankTransfer.accountNumber())) {
            tx.setStatus(BankTransactionStatus.UNMATCHED);
            tx.setReason("Tiền không chuyển vào tài khoản của cửa hàng");
        } else if (orderId == null) {
            tx.setStatus(BankTransactionStatus.UNMATCHED);
            tx.setReason("Nội dung chuyển khoản không có mã đơn hàng");
        } else {
            Optional<Order> order = orderRepository.findByIdForUpdate(orderId);
            if (order.isEmpty()) {
                tx.setStatus(BankTransactionStatus.UNMATCHED);
                tx.setReason("Không có đơn hàng %s".formatted(OrderMapper.code(orderId)));
            } else {
                tx.setOrder(order.get());
                String problem = tryConfirm(tx, order.get(), false, null, now);
                tx.setStatus(problem == null ? BankTransactionStatus.MATCHED : BankTransactionStatus.REVIEW);
                tx.setReason(problem);
            }
        }
        bankTransactionRepository.saveAndFlush(tx);
        log.info("Bank transaction {} ({} VND) stored as {}{}", externalId, tx.getAmount(), tx.getStatus(),
                tx.getReason() == null ? "" : ": " + tx.getReason());
        return new BankWebhookResponse(false, tx.getStatus(),
                tx.getOrder() == null ? null : OrderMapper.code(tx.getOrder().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BankTransactionResponse> search(BankTransactionStatus status, boolean attentionOnly, Pageable pageable) {
        Page<BankTransaction> page;
        if (status != null) {
            page = bankTransactionRepository.findAllByStatusIn(EnumSet.of(status), pageable);
        } else if (attentionOnly) {
            page = bankTransactionRepository.findAllByStatusIn(ATTENTION, pageable);
        } else {
            page = bankTransactionRepository.findAllBy(pageable);
        }
        return PageResponse.from(page.map(BankTransactionServiceImpl::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BankReconciliationInfoResponse info() {
        return new BankReconciliationInfoResponse(webhook.configured(), webhook.simulatorEnabled(),
                bankTransfer.isDemoAccount(), bankTransfer.bankName(), mask(bankTransfer.accountNumber()),
                bankTransactionRepository.countByStatusIn(ATTENTION));
    }

    @Override
    @Transactional
    public BankTransactionResponse assign(Long adminId, Long id, BankTransactionAssignRequest request) {
        User admin = currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        BankTransaction tx = lockPending(id);
        Order order = orderRepository.findByIdForUpdate(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, request.orderId()));
        LocalDateTime now = LocalDateTime.now(clock);
        String problem = tryConfirm(tx, order, true, admin, now);
        if (problem != null) {
            throw new BusinessException(ErrorCode.INVALID_BANK_TRANSACTION, problem);
        }
        tx.setOrder(order);
        tx.setStatus(BankTransactionStatus.RESOLVED);
        tx.setReason(null);
        tx.setResolvedBy(admin);
        tx.setResolvedAt(now);
        tx.setNote(blankToNull(request.note()));
        log.info("Admin id={} applied bank transaction id={} to order id={}", adminId, id, order.getId());
        return toResponse(tx);
    }

    @Override
    @Transactional
    public BankTransactionResponse ignore(Long adminId, Long id, BankTransactionIgnoreRequest request) {
        User admin = currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        BankTransaction tx = lockPending(id);
        tx.setStatus(BankTransactionStatus.IGNORED);
        tx.setResolvedBy(admin);
        tx.setResolvedAt(LocalDateTime.now(clock));
        tx.setNote(request.note().trim());
        log.info("Admin id={} ignored bank transaction id={}", adminId, id);
        return toResponse(tx);
    }

    /** Transactional here: the call to receive() below is internal, so its own @Transactional would not apply. */
    @Override
    @Transactional
    public BankWebhookResponse simulate(BankSimulateRequest request) {
        if (!webhook.simulatorEnabled()) {
            throw new BusinessException(ErrorCode.SIMULATOR_DISABLED);
        }
        String id = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        BankWebhookRequest fake = new BankWebhookRequest(id, "SIMULATOR", LocalDateTime.now(clock).format(BANK_TIME),
                bankTransfer.accountNumber(), request.content().trim(), "in", request.amount());
        return receive(fake, "SIMULATOR");
    }

    @Override
    @Transactional
    public BankWebhookResponse demoConfirm(Long userId, Long orderId) {
        if (!webhook.simulatorEnabled()) {
            throw new BusinessException(ErrorCode.SIMULATOR_DISABLED);
        }
        Order order = orderRepository.findById(orderId)
                .filter(found -> found.getUser() != null && found.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
        Payment payment = paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
        String code = OrderMapper.code(orderId);
        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER || payment.getStatus() != PaymentStatus.PENDING) {
            // sent twice (or the order was cancelled): nothing to pay, keep the reconciliation list clean
            return new BankWebhookResponse(true, null, code);
        }
        return simulate(new BankSimulateRequest(code, payment.getAmount()));
    }

    /**
     * Confirms the order's bank transfer with this money, or returns why that is not possible. Runs inside the
     * caller's transaction with the order row locked. {@code staff} is null for the webhook (no person confirmed).
     */
    private String tryConfirm(BankTransaction tx, Order order, boolean acceptOtherAmount, User staff, LocalDateTime now) {
        Optional<Payment> found = paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId());
        String code = OrderMapper.code(order.getId());
        if (found.isEmpty() || found.get().getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            return "Đơn %s không thanh toán bằng chuyển khoản".formatted(code);
        }
        Payment payment = found.get();
        if (payment.getStatus() == PaymentStatus.PAID) {
            return "Đơn %s đã được thanh toán trước đó (có thể chuyển thừa)".formatted(code);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return "Đơn %s đã huỷ hoặc đã hoàn tiền (khoản thanh toán: %s), cần hoàn tiền cho khách"
                    .formatted(code, payment.getStatus());
        }
        if (!acceptOtherAmount && payment.getAmount().compareTo(tx.getAmount()) != 0) {
            return "Số tiền chuyển %s khác số tiền của đơn %s".formatted(plain(tx.getAmount()), plain(payment.getAmount()));
        }
        orderPaymentLifecycle.confirmTransfer(order, staff, tx.getExternalId(), now);
        return null;
    }

    private BankTransaction lockPending(Long id) {
        BankTransaction tx = bankTransactionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BANK_TRANSACTION_NOT_FOUND, id));
        if (!tx.getStatus().needsAttention()) {
            throw new BusinessException(ErrorCode.INVALID_BANK_TRANSACTION,
                    "Giao dịch đã ở trạng thái %s, không xử lý lại được".formatted(tx.getStatus()));
        }
        return tx;
    }

    private static Long orderIdFrom(String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = ORDER_CODE.matcher(content);
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    private static LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), BANK_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BankTransactionResponse toResponse(BankTransaction tx) {
        Long orderId = tx.getOrder() == null ? null : tx.getOrder().getId();
        return new BankTransactionResponse(tx.getId(), tx.getExternalId(), tx.getProvider(), tx.getAmount(),
                tx.getContent(), tx.getAccountNumber(), tx.getTransferredAt(), tx.getReceivedAt(), tx.getStatus(),
                tx.getReason(), orderId, orderId == null ? null : OrderMapper.code(orderId), tx.getResolvedAt(),
                tx.getNote());
    }

    private static String mask(String accountNumber) {
        int keep = Math.min(3, accountNumber.length());
        return "*".repeat(accountNumber.length() - keep) + accountNumber.substring(accountNumber.length() - keep);
    }

    private static String plain(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString() + "đ";
    }

    private static String truncate(String value, int max) {
        return value == null ? null : value.length() > max ? value.substring(0, max) : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
