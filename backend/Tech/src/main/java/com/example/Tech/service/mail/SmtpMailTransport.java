package com.example.Tech.service.mail;

import com.example.Tech.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
class SmtpMailTransport implements MailTransport {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    @Value("${spring.mail.username:}")
    private String username;

    @Override
    public boolean send(MailMessage message) throws Exception {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.fromAddress() != null ? properties.fromAddress() : username, properties.fromName());
        helper.setTo(message.to());
        helper.setSubject(message.subject());
        helper.setText(message.text(), message.html());
        mailSender.send(mime);
        return true;
    }
}
