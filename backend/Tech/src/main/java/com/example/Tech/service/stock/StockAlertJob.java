package com.example.Tech.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Runs the back-in-stock check every 10 minutes (app.stock-alerts.cron); off when app.stock-alerts.enabled=false (tests). */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.stock-alerts.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StockAlertJob {

    private final StockSubscriptionService subscriptionService;

    @Scheduled(cron = "${app.stock-alerts.cron:0 */10 * * * *}")
    public void run() {
        try {
            subscriptionService.run();
        } catch (RuntimeException e) {
            log.warn("Back-in-stock check failed: {}", e.toString());
        }
    }
}
