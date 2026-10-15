package com.example.Tech.dto.request.contact;

import com.example.Tech.entity.contact.ContactStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Staff update: the new status and an internal note (required for RESOLVED; empty = keep the current note).
 */
public record ContactRequestUpdateRequest(

        @NotNull(message = "Vui lòng chọn trạng thái")
        @Schema(example = "RESOLVED")
        ContactStatus status,

        @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
        @Schema(example = "Đã gọi lại cho khách, đơn giao ngày mai")
        String staffNote
) {
}
