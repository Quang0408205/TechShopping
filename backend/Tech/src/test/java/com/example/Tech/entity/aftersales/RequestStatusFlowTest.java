package com.example.Tech.entity.aftersales;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestStatusFlowTest {

    @Test
    void repairFlow_onlyForwardSteps_rejectFromOpenOnes_noStaffCancel() {
        assertThat(next(ServiceRequestStatus.PENDING)).containsExactly(ServiceRequestStatus.RECEIVED, ServiceRequestStatus.REJECTED);
        assertThat(next(ServiceRequestStatus.RECEIVED)).containsExactly(ServiceRequestStatus.PROCESSING, ServiceRequestStatus.REJECTED);
        assertThat(next(ServiceRequestStatus.PROCESSING)).containsExactly(ServiceRequestStatus.COMPLETED, ServiceRequestStatus.REJECTED);
        assertThat(next(ServiceRequestStatus.COMPLETED)).isEmpty();
        assertThat(next(ServiceRequestStatus.REJECTED)).isEmpty();
        assertThat(next(ServiceRequestStatus.CANCELLED)).isEmpty();
        assertThat(Arrays.stream(ServiceRequestStatus.values()).filter(ServiceRequestStatus::canBeCancelledByCustomer))
                .containsExactly(ServiceRequestStatus.PENDING);
    }

    @Test
    void returnFlow_refundOnlyAfterReceiving_rejectBeforeReceiving() {
        assertThat(next(ReturnStatus.PENDING)).containsExactly(ReturnStatus.APPROVED, ReturnStatus.REJECTED);
        assertThat(next(ReturnStatus.APPROVED)).containsExactly(ReturnStatus.RECEIVED, ReturnStatus.REJECTED);
        assertThat(next(ReturnStatus.RECEIVED)).containsExactly(ReturnStatus.REFUNDED);
        assertThat(next(ReturnStatus.REFUNDED)).isEmpty();
        assertThat(Arrays.stream(ReturnStatus.values()).filter(ReturnStatus::holdsQuantity))
                .containsExactly(ReturnStatus.PENDING, ReturnStatus.APPROVED, ReturnStatus.RECEIVED, ReturnStatus.REFUNDED);
    }

    private static List<ServiceRequestStatus> next(ServiceRequestStatus from) {
        return Arrays.stream(ServiceRequestStatus.values()).filter(from::canMoveTo).toList();
    }

    private static List<ReturnStatus> next(ReturnStatus from) {
        return Arrays.stream(ReturnStatus.values()).filter(from::canMoveTo).toList();
    }
}
