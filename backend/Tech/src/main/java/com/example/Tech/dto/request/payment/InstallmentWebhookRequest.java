package com.example.Tech.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** What an installment partner sends when it has decided an application. */
public record InstallmentWebhookRequest(

        @Schema(description = "Unique id of this event; the same id twice is applied once", example = "evt-1001")
        @NotBlank(message = "eventId là bắt buộc")
        @Size(max = 80, message = "eventId tối đa 80 ký tự")
        String eventId,

        @Schema(description = "Reference the partner gave to the application when it was submitted", example = "CC-1A2B3C4D5E")
        @NotBlank(message = "partnerRef là bắt buộc")
        @Size(max = 60, message = "partnerRef tối đa 60 ký tự")
        String partnerRef,

        @Schema(description = "APPROVED or REJECTED", example = "APPROVED")
        @NotNull(message = "decision là bắt buộc")
        @Pattern(regexp = "APPROVED|REJECTED", message = "decision phải là APPROVED hoặc REJECTED")
        String decision,

        @Schema(description = "Why the application was rejected")
        @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
        String reason
) {
}
