package com.example.Tech.service.mail;

import com.example.Tech.config.MailProperties;
import com.example.Tech.entity.mail.EmailLog;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.mail.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** Mails a customer that products they waited for are back in stock. Never throws; logs to email_log. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockMailService {

    public static final String KIND = "STOCK_AVAILABLE";

    private final EmailLogRepository emailLogRepository;
    private final MailTransport transport;
    private final MailRateLimiter rateLimiter;
    private final MailProperties properties;

    /** true when a mail really went out. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sendBackInStock(User user, List<StockMailTemplates.Item> items) {
        String recipient = user.getEmail();
        String subject = KIND;
        try {
            if (items.isEmpty() || !properties.enabled() || !StringUtils.hasText(recipient)) {
                return false;
            }
            MailMessage message = StockMailTemplates.build(user.getFullname(), recipient, items, properties.siteUrl());
            subject = message.subject();
            if (ReservedAddresses.isReserved(recipient)) {
                emailLogRepository.save(new EmailLog(null, KIND, recipient, subject, EmailLog.SKIPPED, ReservedAddresses.SKIP_REASON));
                return false;
            }
            if (!rateLimiter.tryAcquire(recipient)) {
                emailLogRepository.save(new EmailLog(null, KIND, recipient, subject, EmailLog.SKIPPED,
                        "Too many mails to this address in the current window"));
                return false;
            }
            boolean sent = transport.send(message);
            emailLogRepository.save(new EmailLog(null, KIND, recipient, subject, sent ? EmailLog.SENT : EmailLog.SKIPPED,
                    sent ? null : "SMTP is not configured (MAIL_USERNAME is empty)"));
            return sent;
        } catch (Exception e) {
            log.warn("Back-in-stock mail to user id={} failed: {}", user.getId(), e.toString());
            try {
                emailLogRepository.save(new EmailLog(null, KIND, recipient == null ? "-" : recipient, subject,
                        EmailLog.FAILED, e.toString()));
            } catch (RuntimeException ignored) {
                // the log itself failed
            }
            return false;
        }
    }
}
