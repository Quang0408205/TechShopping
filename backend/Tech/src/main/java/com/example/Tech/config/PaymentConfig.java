package com.example.Tech.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.ApplicationRunner;

@Slf4j
@Configuration
@EnableConfigurationProperties({BankTransferProperties.class, PaymentWebhookProperties.class, StripeProperties.class,
        InstallmentProperties.class})
public class PaymentConfig {

    /** The QR must point to the real account: say so at start-up when the demo account is still in use. */
    @Bean
    ApplicationRunner bankAccountCheck(BankTransferProperties bank, PaymentWebhookProperties webhook, StripeProperties stripe) {
        return args -> {
            if (!stripe.enabled()) {
                log.warn("STRIPE_SECRET_KEY is not set: the card payment method (Stripe) is hidden");
            } else if (!stripe.testMode()) {
                log.warn("STRIPE_SECRET_KEY is a LIVE key: real cards will be charged");
            }
            if (bank.isDemoAccount()) {
                log.warn("PAYMENT_BANK_ACCOUNT_NUMBER is not set: the transfer QR shows the DEMO account. "
                        + "Set PAYMENT_BANK_NAME / PAYMENT_BANK_BIN / PAYMENT_BANK_ACCOUNT_NUMBER / PAYMENT_BANK_ACCOUNT_NAME in .env");
            }
            if (!webhook.configured()) {
                log.warn("PAYMENT_WEBHOOK_SECRET is not set: the bank webhook is switched off (503)");
            }
        };
    }
}
