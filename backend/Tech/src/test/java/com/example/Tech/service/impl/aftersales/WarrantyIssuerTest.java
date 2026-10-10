package com.example.Tech.service.impl.aftersales;

import com.example.Tech.entity.aftersales.Warranty;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.repository.aftersales.WarrantyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantyIssuerTest {

    private static final LocalDate DELIVERED_ON = LocalDate.of(2026, 1, 31);

    @Mock
    private WarrantyRepository warrantyRepository;

    private WarrantyIssuer warrantyIssuer;

    @BeforeEach
    void setUp() {
        warrantyIssuer = new WarrantyIssuer(warrantyRepository);
    }

    @Test
    void issueFor_onePerLineWithWarrantyMonths_skipsZeroMonthsAndLinesAlreadyIssued() {
        OrderItem phone = item(1L, 12);
        OrderItem cable = item(2L, 0);
        OrderItem laptop = item(3L, 24);
        OrderItem watch = item(4L, 6);
        Order order = new Order();
        List.of(phone, cable, laptop, watch).forEach(order::addItem);
        Warranty existing = new Warranty();
        existing.setOrderItem(watch);
        when(warrantyRepository.findAllByOrderItem_IdIn(any())).thenReturn(List.of(existing));
        when(warrantyRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        warrantyIssuer.issueFor(order, DELIVERED_ON);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Warranty>> saved = ArgumentCaptor.forClass(List.class);
        verify(warrantyRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(w -> w.getOrderItem().getId()).containsExactly(1L, 3L);
        assertThat(saved.getValue().get(0).getStartDate()).isEqualTo(DELIVERED_ON);
        // month arithmetic clamps to the last day of a shorter month
        assertThat(saved.getValue().get(0).getEndDate()).isEqualTo(LocalDate.of(2027, 1, 31));
        assertThat(saved.getValue().get(1).getEndDate()).isEqualTo(LocalDate.of(2028, 1, 31));
        assertThat(saved.getValue().get(0).getWarrantyType()).isEqualTo(Warranty.TYPE_STANDARD);
    }

    @Test
    void isValidOn_includesBothEnds() {
        Warranty warranty = new Warranty();
        warranty.setStartDate(DELIVERED_ON);
        warranty.setEndDate(DELIVERED_ON.plusMonths(12));

        assertThat(warranty.isValidOn(DELIVERED_ON)).isTrue();
        assertThat(warranty.isValidOn(DELIVERED_ON.plusMonths(12))).isTrue();
        assertThat(warranty.isValidOn(DELIVERED_ON.plusMonths(12).plusDays(1))).isFalse();
        warranty.setActive(false);
        assertThat(warranty.isValidOn(DELIVERED_ON)).isFalse();
    }

    private static OrderItem item(Long id, int warrantyMonths) {
        Product product = new Product();
        product.setWarrantyMonths(warrantyMonths);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setVariant(variant);
        return item;
    }
}
