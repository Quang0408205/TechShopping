package com.example.Tech.dto.request.payment;

import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.service.payment.InstallmentPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Installment application sent with an INSTALLMENT order. The partner, its terms and the card bank are checked by the
 * service, because they depend on the partner chosen.
 */
public record InstallmentRequest(

        @Schema(description = "A term the chosen partner supports (see GET /payments/installment-options)", example = "6")
        @NotNull(message = "Vui lòng chọn kỳ hạn trả góp")
        Integer months,

        @Schema(description = "Citizen id: exactly " + InstallmentPolicy.CITIZEN_ID_LENGTH + " digits", example = "0123456789")
        @NotBlank(message = "Vui lòng nhập số CCCD")
        @Pattern(regexp = "^\\s*[0-9]{" + InstallmentPolicy.CITIZEN_ID_LENGTH + "}\\s*$",
                message = "Số CCCD phải gồm đúng " + InstallmentPolicy.CITIZEN_ID_LENGTH + " chữ số")
        String citizenId,

        @Schema(description = "Bank that issued the credit card; only for the credit card partner", example = "VCB")
        InstallmentBank cardBank,

        @Schema(description = "Partner code: CREDIT_CARD, FINANCE_A, FINANCE_B. Left out with a card bank = CREDIT_CARD",
                example = "CREDIT_CARD")
        @Size(max = 30, message = "Mã đối tác tối đa 30 ký tự")
        String partner
) {

    public InstallmentRequest(Integer months, String citizenId, InstallmentBank cardBank) {
        this(months, citizenId, cardBank, null);
    }
}
