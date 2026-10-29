package com.example.Tech.dto.request.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BankTransactionAssignRequest(

        @NotNull(message = "Vui lòng chọn đơn hàng")
        Long orderId,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {
}
