package com.example.Tech.service.mail;

import com.example.Tech.config.MailProperties;
import com.example.Tech.entity.mail.EmailLog;
import com.example.Tech.entity.order.Order;
import com.example.Tech.repository.mail.EmailLogRepository;
import com.example.Tech.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds the mail of an order and sends it, then writes email_log. Never throws: a mail problem must not reach the
 * caller (the order is already committed). Order without an address, mail switched off or no SMTP user = SKIPPED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMailService {

    private final OrderRepository orderRepository;
    private final EmailLogRepository emailLogRepository;
    private final MailTransport transport;
    private final MailRateLimiter rateLimiter;
    private final MailProperties properties;

    /** REQUIRES_NEW: when run on the committing thread the order's own transaction is still bound to it. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Long orderId, MailKind kind) {
        String recipient = null;
        String subject = kind.name();
        try {
            Order order = orderRepository.findWithUserById(orderId).orElse(null);
            if (order == null) {
                return;
            }
            recipient = recipientOf(order);
            if (kind == MailKind.ORDER_CANCELLED && OrderMailPublisher.paidOnline(order)
                    && !emailLogRepository.existsByOrderIdAndKindAndStatus(orderId, MailKind.PAYMENT_RECEIVED.name(), EmailLog.SENT)) {
                // never mailed about it (cancelled before paying, e.g. "Huỷ thanh toán"): no cancellation mail either
                return;
            }
            if (!properties.enabled() || !StringUtils.hasText(recipient)) {
                emailLogRepository.save(new EmailLog(orderId, kind.name(), recipient == null ? "-" : recipient, subject,
                        EmailLog.SKIPPED, !properties.enabled() ? "Mail is switched off" : "The order has no email address"));
                return;
            }
            MailMessage message = OrderMailTemplates.build(kind, order, recipient, trackUrl(order));
            subject = message.subject();
            if (!rateLimiter.tryAcquire(recipient)) {
                emailLogRepository.save(new EmailLog(orderId, kind.name(), recipient, subject, EmailLog.SKIPPED,
                        "Too many mails to this address in the current window"));
                log.warn("Mail {} of order id={} skipped: recipient over its limit", kind, orderId);
                return;
            }
            boolean sent = transport.send(message);
            emailLogRepository.save(new EmailLog(orderId, kind.name(), recipient, subject,
                    sent ? EmailLog.SENT : EmailLog.SKIPPED, sent ? null : "SMTP is not configured (MAIL_USERNAME is empty)"));
        } catch (Exception e) {
            log.warn("Mail {} of order id={} failed: {}", kind, orderId, e.toString());
            try {
                emailLogRepository.save(new EmailLog(orderId, kind.name(), recipient == null ? "-" : recipient, subject,
                        EmailLog.FAILED, e.toString()));
            } catch (RuntimeException ignored) {
                // the log itself failed: nothing more to do
            }
        }
    }

    /** The address typed at checkout, else the account's own. */
    private static String recipientOf(Order order) {
        return StringUtils.hasText(order.getContactEmail()) ? order.getContactEmail().trim() : order.getUser().getEmail();
    }

    private String trackUrl(Order order) {
        return properties.siteUrl() + "/customer/track.html?token="
                + URLEncoder.encode(order.getTrackingToken(), StandardCharsets.UTF_8);
    }
}
