package com.example.Tech.service.payment.installment;

import com.example.Tech.config.InstallmentProperties;
import com.example.Tech.service.payment.InstallmentDecisionService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Makes the simulated partner answer by itself a few seconds after the application is saved, as a real partner would
 * call the webhook. Switched off when {@code app.payment.installment.auto-decide-seconds} is negative.
 */
@Slf4j
@Component
public class InstallmentAutoDecider {

    private final InstallmentProperties properties;
    private final ObjectProvider<InstallmentDecisionService> decisions;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "installment-partner");
        thread.setDaemon(true);
        return thread;
    });

    public InstallmentAutoDecider(InstallmentProperties properties, ObjectProvider<InstallmentDecisionService> decisions) {
        this.properties = properties;
        this.decisions = decisions;
    }

    /** Schedules the answer once the transaction that saved the application is committed. */
    public void scheduleAfterCommit(Long orderId) {
        if (!properties.autoDecide()) {
            return;
        }
        Runnable schedule = () -> executor.schedule(() -> answer(orderId), properties.autoDecideSeconds(), TimeUnit.SECONDS);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    schedule.run();
                }
            });
        } else {
            schedule.run();
        }
    }

    private void answer(Long orderId) {
        try {
            decisions.getObject().decideAsPartner(orderId);
        } catch (RuntimeException e) {
            log.warn("The simulated partner could not answer the application of order id={}: {}", orderId, e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
