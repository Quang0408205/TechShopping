package com.example.Tech.dto.request.order;

import com.example.Tech.dto.request.payment.InstallmentRequest;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

        @Schema(description = "Required for HOME_DELIVERY (the district / city in it picks the nearest store); "
                + "ignored for PICKUP", example = "12 Nguyễn Trãi, Phường 3, Quận 5, TP. Hồ Chí Minh")
        @Size(max = 1000, message = "Shipping address must be at most 1000 characters")
        String shippingAddress,

        @Schema(description = "Optional note for the shop")
        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String note,

        @Schema(description = "COD, BANK_TRANSFER, INSTALLMENT or CARD (Stripe, test mode)", example = "COD")
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Schema(description = "Required when paymentMethod is INSTALLMENT, must be absent otherwise")
        @Valid
        InstallmentRequest installment,

        @Schema(description = "HOME_DELIVERY (default when absent) or PICKUP", example = "HOME_DELIVERY")
        DeliveryType deliveryType,

        @Schema(description = "Open store to pick the order up at; required for PICKUP, must be absent otherwise",
                example = "1")
        Integer pickupStoreId,

        @Schema(description = "Optional, HOME_DELIVERY only: province / city of the 2-level address (since 2025-07-01)",
                example = "Thành phố Hồ Chí Minh")
        @Size(max = 100, message = "Shipping province must be at most 100 characters")
        String shippingProvince,

        @Schema(description = "Optional, HOME_DELIVERY only: ward / commune", example = "Phường Chợ Quán")
        @Size(max = 100, message = "Shipping ward must be at most 100 characters")
        String shippingWard,

        @Schema(description = "Optional voucher code; re-checked against the cart when the order is placed",
                example = "POY10")
        @Size(max = 30, message = "Mã voucher tối đa 30 ký tự")
        String voucherCode,

        @Schema(description = "Optional loyalty points to spend: 1 point = 1.000đ, at most 10% of the goods "
                + "after the voucher and at most the balance", example = "200")
        @Min(value = 0, message = "Số điểm không được âm")
        @Max(value = 1_000_000, message = "Số điểm quá lớn")
        Integer pointsToUse,

        @Schema(description = "Where the order mails go (placed, confirmed, shipping…); when absent the account's "
                + "email is used", example = "an@example.com")
        @Email(message = "Email chưa hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String contactEmail
) {

    public OrderCreateRequest(String recipientName, String recipientPhone, String shippingAddress, String note,
                              PaymentMethod paymentMethod, InstallmentRequest installment,
                              DeliveryType deliveryType, Integer pickupStoreId,
                              String shippingProvince, String shippingWard, String voucherCode, Integer pointsToUse) {
        this(recipientName, recipientPhone, shippingAddress, note, paymentMethod, installment, deliveryType,
                pickupStoreId, shippingProvince, shippingWard, voucherCode, pointsToUse, null);
    }

    public OrderCreateRequest(String recipientName, String recipientPhone, String shippingAddress, String note,
                              PaymentMethod paymentMethod, InstallmentRequest installment,
                              DeliveryType deliveryType, Integer pickupStoreId,
                              String shippingProvince, String shippingWard, String voucherCode) {
        this(recipientName, recipientPhone, shippingAddress, note, paymentMethod, installment, deliveryType,
                pickupStoreId, shippingProvince, shippingWard, voucherCode, null);
    }

    public OrderCreateRequest(String recipientName, String recipientPhone, String shippingAddress, String note,
                              PaymentMethod paymentMethod, InstallmentRequest installment,
                              DeliveryType deliveryType, Integer pickupStoreId,
                              String shippingProvince, String shippingWard) {
        this(recipientName, recipientPhone, shippingAddress, note, paymentMethod, installment, deliveryType,
                pickupStoreId, shippingProvince, shippingWard, null);
    }

    public OrderCreateRequest(String recipientName, String recipientPhone, String shippingAddress, String note,
                              PaymentMethod paymentMethod, InstallmentRequest installment,
                              DeliveryType deliveryType, Integer pickupStoreId) {
        this(recipientName, recipientPhone, shippingAddress, note, paymentMethod, installment, deliveryType,
                pickupStoreId, null, null);
    }

    /** Home delivery checkout with installment data (or null). */
    public OrderCreateRequest(String recipientName, String recipientPhone, String shippingAddress, String note,
                              PaymentMethod paymentMethod, InstallmentRequest installment) {
        this(recipientName, recipientPhone, shippingAddress, note, paymentMethod, installment, null, null);
    }

    /** Home delivery checkout without installment data (COD / BANK_TRANSFER). */
    public OrderCreateRequest(String recipientName, String recipientPhone, String shippingAddress, String note,
                              PaymentMethod paymentMethod) {
        this(recipientName, recipientPhone, shippingAddress, note, paymentMethod, null);
    }

    /** deliveryType with the default applied. */
    public DeliveryType deliveryTypeOrDefault() {
        return deliveryType == null ? DeliveryType.HOME_DELIVERY : deliveryType;
    }
}
