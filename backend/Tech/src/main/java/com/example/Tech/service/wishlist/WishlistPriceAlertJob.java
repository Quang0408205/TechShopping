package com.example.Tech.service.wishlist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Runs the wishlist price check every hour (app.wishlist.alert-cron); off when app.wishlist.alerts-enabled=false (tests). */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.wishlist.alerts-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WishlistPriceAlertJob {

    private final WishlistPriceAlertService alertService;

    @Scheduled(cron = "${app.wishlist.alert-cron:0 0 * * * *}")
    public void run() {
        try {
            alertService.run();
        } catch (RuntimeException e) {
            log.warn("Wishlist price check failed: {}", e.toString());
        }
    }
}
