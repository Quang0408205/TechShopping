package com.example.Tech.service.mail;

import com.example.Tech.config.MailProperties;
import com.example.Tech.entity.mail.EmailLog;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.mail.EmailLogRepository;
import com.example.Tech.repository.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMailTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private MailTransport transport;

    @Mock
    private MailRateLimiter rateLimiter;

    private MailProperties properties;
    private OrderMailService service;

    @BeforeEach
    void setUp() {
        properties = new MailProperties(true, "POY", null, "https://poy.example/", 12, Duration.ofHours(1), false);
        service = new OrderMailService(orderRepository, emailLogRepository, transport, rateLimiter, properties);
    }

    @Test
    void subject_followsTheStatus_andHasThePoyPrefix() {
        Order order = order();
        assertThat(subject(MailKind.ORDER_PLACED, order)).isEqualTo("[POY] Cảm ơn bạn đã đặt hàng – Đơn DH00000042");
        assertThat(subject(MailKind.PAYMENT_RECEIVED, order)).isEqualTo("[POY] Chúng tôi đã nhận thanh toán đơn DH00000042");
        assertThat(subject(MailKind.ORDER_CONFIRMED, order)).isEqualTo("[POY] Đơn hàng DH00000042 đã được xác nhận");
        assertThat(subject(MailKind.ORDER_SHIPPING, order))
                .isEqualTo("[POY] Đơn hàng DH00000042 của bạn đang được vận chuyển");
        assertThat(subject(MailKind.READY_FOR_PICKUP, order)).isEqualTo("[POY] Đơn hàng DH00000042 đã sẵn sàng tại cửa hàng");
        assertThat(subject(MailKind.ORDER_DELIVERED, order)).isEqualTo("[POY] Đơn hàng DH00000042 đã giao thành công");
        assertThat(subject(MailKind.ORDER_CANCELLED, order)).isEqualTo("[POY] Đơn hàng DH00000042 đã được huỷ");
    }

    @Test
    void body_hasTheProductsAndTheMoney_butNeitherThePhoneNorTheStreet_andEscapesHtml() {
        Order order = order();
        MailMessage message = OrderMailTemplates.build(MailKind.ORDER_PLACED, order, "an@example.com",
                "https://poy.example/customer/track.html?token=abc");

        for (String body : new String[]{message.html(), message.text()}) {
            assertThat(body).contains("Điện thoại X - 128GB").contains("29.980.000đ").contains("Phường 3")
                    .contains("DH00000042").contains("customer/track.html?token=abc")
                    .doesNotContain("0901234567").doesNotContain("12 Nguyễn Trãi");
        }
        assertThat(message.html()).contains("&lt;b&gt;Quang&lt;/b&gt;").doesNotContain("<b>Quang</b>");
    }

    @Test
    void send_logsSent_whenTheTransportSends() throws Exception {
        when(orderRepository.findWithUserById(42L)).thenReturn(Optional.of(order()));
        when(rateLimiter.tryAcquire("an@example.com")).thenReturn(true);
        when(transport.send(any())).thenReturn(true);

        service.send(42L, MailKind.ORDER_PLACED);

        EmailLog log = logged();
        assertThat(log.getStatus()).isEqualTo(EmailLog.SENT);
        assertThat(log.getRecipient()).isEqualTo("an@example.com");
        assertThat(log.getOrderId()).isEqualTo(42L);
    }

    @Test
    void send_usesTheAccountEmail_whenTheOrderHasNone() throws Exception {
        Order order = order();
        order.setContactEmail(null);
        when(orderRepository.findWithUserById(42L)).thenReturn(Optional.of(order));
        when(rateLimiter.tryAcquire("tai.khoan@example.com")).thenReturn(true);
        when(transport.send(any())).thenReturn(true);

        service.send(42L, MailKind.ORDER_CONFIRMED);

        ArgumentCaptor<MailMessage> sent = ArgumentCaptor.forClass(MailMessage.class);
        verify(transport).send(sent.capture());
        assertThat(sent.getValue().to()).isEqualTo("tai.khoan@example.com");
    }

    @Test
    void send_isSkipped_whenSmtpIsNotConfigured_orTheRecipientIsOverTheLimit_orMailIsOff() throws Exception {
        when(orderRepository.findWithUserById(42L)).thenReturn(Optional.of(order()));
        when(rateLimiter.tryAcquire("an@example.com")).thenReturn(true, false);
        when(transport.send(any())).thenReturn(false);

        service.send(42L, MailKind.ORDER_PLACED);
        assertThat(logged().getStatus()).isEqualTo(EmailLog.SKIPPED);

        service.send(42L, MailKind.ORDER_PLACED);
        verify(transport).send(any());

        MailProperties off = new MailProperties(false, "POY", null, null, 12, null, false);
        new OrderMailService(orderRepository, emailLogRepository, transport, rateLimiter, off)
                .send(42L, MailKind.ORDER_PLACED);
        verify(transport).send(any());
    }

    @Test
    void send_neverThrows_andLogsFailed_whenTheProviderRefuses() throws Exception {
        when(orderRepository.findWithUserById(42L)).thenReturn(Optional.of(order()));
        when(rateLimiter.tryAcquire("an@example.com")).thenReturn(true);
        when(transport.send(any())).thenThrow(new IllegalStateException("535 bad credentials"));

        service.send(42L, MailKind.ORDER_SHIPPING);

        EmailLog log = logged();
        assertThat(log.getStatus()).isEqualTo(EmailLog.FAILED);
        assertThat(log.getError()).contains("535 bad credentials");
    }

    @Test
    void placedMail_isNotRequestedForQrCardOrInstallmentOrders_butIsForCod() {
        org.springframework.context.ApplicationEventPublisher events = org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class);
        OrderMailPublisher publisher = new OrderMailPublisher(events);
        Order order = order();

        order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        publisher.statusChanged(order, OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.CARD);
        publisher.statusChanged(order, OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.INSTALLMENT);
        publisher.statusChanged(order, OrderStatus.PENDING);
        verify(events, never()).publishEvent(any(Object.class));

        order.setPaymentMethod(PaymentMethod.COD);
        publisher.statusChanged(order, OrderStatus.PENDING);
        verify(events, org.mockito.Mockito.times(1)).publishEvent(any(Object.class));

        order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        publisher.statusChanged(order, OrderStatus.CONFIRMED);
        publisher.paymentReceived(order);
        verify(events, org.mockito.Mockito.times(3)).publishEvent(any(Object.class));
    }

    @Test
    void cancelledMail_isSkippedForAnOnlineOrderThatWasNeverPaid_butSentOnceItWas() throws Exception {
        Order order = order();
        order.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        when(orderRepository.findWithUserById(42L)).thenReturn(Optional.of(order));
        when(emailLogRepository.existsByOrderIdAndKindAndStatus(42L, "PAYMENT_RECEIVED", EmailLog.SENT)).thenReturn(false, true);
        when(rateLimiter.tryAcquire("an@example.com")).thenReturn(true);
        when(transport.send(any())).thenReturn(true);

        service.send(42L, MailKind.ORDER_CANCELLED);
        verify(transport, never()).send(any());
        verify(emailLogRepository, never()).save(any());

        service.send(42L, MailKind.ORDER_CANCELLED);
        verify(transport).send(any());
    }

    @Test
    void send_doesNothing_forAnUnknownOrder() {
        when(orderRepository.findWithUserById(7L)).thenReturn(Optional.empty());

        service.send(7L, MailKind.ORDER_PLACED);

        verify(emailLogRepository, never()).save(any());
    }

    private EmailLog logged() {
        ArgumentCaptor<EmailLog> captor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private static String subject(MailKind kind, Order order) {
        return OrderMailTemplates.build(kind, order, "an@example.com", "https://poy.example/x").subject();
    }

    private static Order order() {
        User user = new User();
        user.setEmail("tai.khoan@example.com");
        Product product = new Product();
        product.setName("Điện thoại X");
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("128GB");

        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 42L);
        order.setUser(user);
        order.setContactEmail("an@example.com");
        order.setRecipientName("<b>Quang</b>");
        order.setRecipientPhone("0901234567");
        order.setShippingAddress("12 Nguyễn Trãi, Phường 3, Quận 5");
        order.setShippingWard("Phường 3");
        order.setShippingProvince("TP. Hồ Chí Minh");
        order.setDeliveryType(DeliveryType.HOME_DELIVERY);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("29980000"));
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("14990000"));
        item.setSubtotal(new BigDecimal("29980000"));
        order.addItem(item);
        return order;
    }
}
