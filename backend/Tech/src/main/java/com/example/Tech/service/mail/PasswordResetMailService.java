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

/** Mails the link that resets a password. Never throws; every attempt is written to email_log. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetMailService {

    public static final String KIND = "PASSWORD_RESET";

    private final EmailLogRepository emailLogRepository;
    private final MailTransport transport;
    private final MailRateLimiter rateLimiter;
    private final MailProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(User user, String token, long validMinutes) {
        String recipient = user.getEmail();
        String subject = PasswordResetMailTemplates.SUBJECT;
        try {
            if (!properties.enabled()) {
                emailLogRepository.save(new EmailLog(null, KIND, recipient, subject, EmailLog.SKIPPED, "Mail is switched off"));
                return;
            }
            if (ReservedAddresses.isReserved(recipient)) {
                emailLogRepository.save(new EmailLog(null, KIND, recipient, subject, EmailLog.SKIPPED,
                        ReservedAddresses.SKIP_REASON));
                return;
            }
            if (!rateLimiter.tryAcquire(recipient)) {
                emailLogRepository.save(new EmailLog(null, KIND, recipient, subject, EmailLog.SKIPPED,
                        "Too many mails to this address in the current window"));
                return;
            }
            String link = properties.siteUrl() + "/auth/reset-password.html?token=" + token;
            MailMessage message = PasswordResetMailTemplates.build(user.getFullname(), recipient, link, validMinutes);
            boolean sent = transport.send(message);
            emailLogRepository.save(new EmailLog(null, KIND, recipient, subject,
                    sent ? EmailLog.SENT : EmailLog.SKIPPED, sent ? null : "SMTP is not configured (MAIL_USERNAME is empty)"));
        } catch (Exception e) {
            log.warn("Password reset mail to user id={} failed: {}", user.getId(), e.toString());
            try {
                emailLogRepository.save(new EmailLog(null, KIND, recipient, subject, EmailLog.FAILED, e.toString()));
            } catch (RuntimeException ignored) {
                // the log itself failed
            }
        }
    }
}
