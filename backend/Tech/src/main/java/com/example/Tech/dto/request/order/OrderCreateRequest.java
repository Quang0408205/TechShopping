package com.example.Tech.dto.request.order;

import com.example.Tech.dto.request.payment.InstallmentRequest;
import com.example.Tech.entity.order.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Checkout. The items, prices and shipping fee are NOT sent: they are taken from the user's server cart.
 */
public record OrderCreateRequest(

        @Schema(example = "Nguyễn Văn An")
        @NotBlank(message = "Recipient name is required")
        @Size(max = 120, message = "Recipient name must be at most 120 characters")
        String recipientName,

        @Schema(description = "8–20 characters: digits, spaces, + ( ) . -", example = "0901 234 567")
        @NotBlank(message = "Recipient phone is required")
        @Pattern(regexp = "^\\s*[0-9+\\s().-]{8,20}\\s*$", message = "Recipient phone must be 8-20 digits or + ( ) . - characters")
        String recipientPhone,

        @Schema(example = "12 Nguyễn Trãi, Phường 3, Quận 5, TP. Hồ Chí Minh")
        @NotBlank(message = "Shipping address is required")
        @Size(max = 1000, message = "Shipping address must be at most 1000 characters")
        String shippingAddress,

        @Schema(description = "Optional note for the shop")
        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String note,

        @Schema(description = "COD, BANK_TRANSFER or INSTALLMENT", example = "COD")
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Schema(description = "Required when paymentMethod is INSTALLMENT, must be absent otherwise")
        @Valid
        InstallmentRequest installment
) {

    /** Checkout without installment data (COD / BANK_TRANSFER). */
    public OrderCreateRequest(String recipientName, String recipientPhone, String shippingAddress, String note,
                              PaymentMethod paymentMethod) {
        this(recipientName, recipientPhone, shippingAddress, note, paymentMethod, null);
    }
}
