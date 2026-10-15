package com.example.Tech.dto.request.payment;

import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.service.payment.InstallmentPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Installment application sent with an INSTALLMENT order. The allowed terms are checked by the service. */
public record InstallmentRequest(

        @Schema(description = "3, 6, 9 or 12", example = "6")
        @NotNull(message = "Vui lòng chọn kỳ hạn trả góp")
        Integer months,

        @Schema(description = "Citizen id: exactly " + InstallmentPolicy.CITIZEN_ID_LENGTH + " digits", example = "0123456789")
        @NotBlank(message = "Vui lòng nhập số CCCD")
        @Pattern(regexp = "^\\s*[0-9]{" + InstallmentPolicy.CITIZEN_ID_LENGTH + "}\\s*$",
                message = "Số CCCD phải gồm đúng " + InstallmentPolicy.CITIZEN_ID_LENGTH + " chữ số")
        String citizenId,

        @Schema(description = "Bank that issued the credit card", example = "VCB")
        @NotNull(message = "Vui lòng chọn ngân hàng phát hành thẻ")
        InstallmentBank cardBank
) {
}
