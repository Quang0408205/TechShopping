package com.example.Tech.service.impl.order;

import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.OrderStatusHistory;
import com.example.Tech.entity.sales.SalesRecord;
import com.example.Tech.repository.employee.EmployeeRepository;
import com.example.Tech.repository.order.OrderStatusHistoryRepository;
import com.example.Tech.repository.sales.SalesRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Runs inside the caller's transaction when an order becomes DELIVERED: credits the whole order total to the
 * employee who confirmed it, with the configured commission rate at that moment (not recomputed later).
 */
@Slf4j
@Component
public class SalesRecorder {

    private final OrderStatusHistoryRepository historyRepository;
    private final EmployeeRepository employeeRepository;
    private final SalesRecordRepository salesRecordRepository;
    private final BigDecimal commissionRate;

    public SalesRecorder(OrderStatusHistoryRepository historyRepository, EmployeeRepository employeeRepository,
                         SalesRecordRepository salesRecordRepository,
                         @Value("${app.sales.commission-rate:0.01}") BigDecimal commissionRate) {
        if (commissionRate.signum() < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalStateException("app.sales.commission-rate must be between 0 and 1, got " + commissionRate);
        }
        this.historyRepository = historyRepository;
        this.employeeRepository = employeeRepository;
        this.salesRecordRepository = salesRecordRepository;
        this.commissionRate = commissionRate;
    }

    /** Empty when nobody with an employee profile confirmed the order (e.g. confirmed before Phase 10). */
    public Optional<SalesRecord> recordFor(Order order, LocalDateTime deliveredAt) {
        Optional<Employee> seller = historyRepository
                .findFirstByOrder_IdAndNewStatusOrderByChangedAtDescIdDesc(order.getId(), OrderStatus.CONFIRMED)
                .map(OrderStatusHistory::getChangedBy)
                .flatMap(user -> employeeRepository.findByUserId(user.getId()));
        if (seller.isEmpty() || order.getStore() == null) {
            log.warn("Order id={} delivered without a confirming employee or a store: no sales record", order.getId());
            return Optional.empty();
        }
        SalesRecord record = new SalesRecord();
        record.setStore(order.getStore());
        record.setEmployee(seller.get());
        record.setOrder(order);
        record.setSalesAmount(order.getTotalAmount());
        record.setCommission(order.getTotalAmount().multiply(commissionRate).setScale(0, RoundingMode.HALF_UP));
        record.setRecordedAt(deliveredAt);
        return Optional.of(salesRecordRepository.save(record));
    }
}
