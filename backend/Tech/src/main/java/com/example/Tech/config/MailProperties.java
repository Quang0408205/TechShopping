package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Order emails. {@code fromAddress} empty = the SMTP username; {@code siteUrl} is where the links of the
 * mails point (the web shop, not the API). One address receives at most {@code perRecipientLimit} mails per window.
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(Boolean enabled, String fromName, String fromAddress, String siteUrl,
                             Integer perRecipientLimit, Duration window, Boolean async) {

    public MailProperties {
        enabled = enabled == null || enabled;
        fromName = StringUtils.hasText(fromName) ? fromName.trim() : "POY";
        fromAddress = StringUtils.hasText(fromAddress) ? fromAddress.trim() : null;
        siteUrl = StringUtils.hasText(siteUrl) ? siteUrl.trim().replaceAll("/+$", "") : "http://localhost:5510";
        perRecipientLimit = perRecipientLimit != null && perRecipientLimit > 0 ? perRecipientLimit : 12;
        window = window != null && !window.isNegative() && !window.isZero() ? window : Duration.ofHours(1);
        async = async == null || async;
    }
}
