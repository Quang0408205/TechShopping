package com.example.Tech.service.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Hands the mail to the mail executor once the order transaction has committed (a rolled back order sends nothing). */
@Slf4j
@Component
class OrderMailListener {

    private final TaskExecutor executor;
    private final OrderMailService mailService;

    OrderMailListener(@Qualifier("mailExecutor") TaskExecutor executor, OrderMailService mailService) {
        this.executor = executor;
        this.mailService = mailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderMail(OrderMailEvent event) {
        try {
            executor.execute(() -> mailService.send(event.orderId(), event.kind()));
        } catch (RuntimeException e) {
            log.warn("Could not queue the {} mail of order id={}: {}", event.kind(), event.orderId(), e.toString());
        }
    }
}
