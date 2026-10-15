package com.example.Tech.service.impl.order;

import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.OrderStatusHistory;
import com.example.Tech.entity.sales.SalesRecord;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.employee.EmployeeRepository;
import com.example.Tech.repository.order.OrderStatusHistoryRepository;
import com.example.Tech.repository.sales.SalesRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesRecorderTest {

    private static final LocalDateTime DELIVERED_AT = LocalDateTime.of(2026, 10, 9, 15, 30);

    @Mock
    private OrderStatusHistoryRepository historyRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SalesRecordRepository salesRecordRepository;

    private final User staff = new User();
    private final Employee employee = new Employee();
    private final Store store = new Store();
    private Order order;

    @BeforeEach
    void setUp() {
        staff.setId(7L);
        order = new Order();
        order.setId(5L);
        order.setStore(store);
        order.setTotalAmount(new BigDecimal("15990050"));
        lenient().when(salesRecordRepository.save(any(SalesRecord.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void delivered_creditsTheConfirmingEmployee_withTheRoundedCommission() {
        confirmedBy(staff);
        when(employeeRepository.findByUserId(7L)).thenReturn(Optional.of(employee));

        SalesRecord record = recorder("0.01").recordFor(order, DELIVERED_AT).orElseThrow();

        assertThat(record.getEmployee()).isSameAs(employee);
        assertThat(record.getStore()).isSameAs(store);
        assertThat(record.getOrder()).isSameAs(order);
        assertThat(record.getSalesAmount()).isEqualByComparingTo("15990050");
        assertThat(record.getCommission()).isEqualByComparingTo("159901");
        assertThat(record.getRecordedAt()).isEqualTo(DELIVERED_AT);
    }

    @Test
    void nobodyConfirmed_orNoEmployeeProfile_noRecord() {
        when(historyRepository.findFirstByOrder_IdAndNewStatusOrderByChangedAtDescIdDesc(5L, OrderStatus.CONFIRMED))
                .thenReturn(Optional.empty());
        assertThat(recorder("0.01").recordFor(order, DELIVERED_AT)).isEmpty();

        confirmedBy(staff);
        when(employeeRepository.findByUserId(7L)).thenReturn(Optional.empty());
        assertThat(recorder("0.01").recordFor(order, DELIVERED_AT)).isEmpty();

        verify(salesRecordRepository, never()).save(any());
    }

    @Test
    void commissionRate_outsideZeroToOne_isRefusedAtStartup() {
        assertThatThrownBy(() -> recorder("1.5")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> recorder("-0.01")).isInstanceOf(IllegalStateException.class);
        assertThat(recorder("0")).isNotNull();
    }

    private void confirmedBy(User user) {
        when(historyRepository.findFirstByOrder_IdAndNewStatusOrderByChangedAtDescIdDesc(5L, OrderStatus.CONFIRMED))
                .thenReturn(Optional.of(new OrderStatusHistory(order, OrderStatus.PENDING, OrderStatus.CONFIRMED, user)));
    }

    private SalesRecorder recorder(String rate) {
        return new SalesRecorder(historyRepository, employeeRepository, salesRecordRepository, new BigDecimal(rate));
    }
}
