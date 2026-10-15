package com.example.Tech.dto.request.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create or update (full replace) a store. city and district are required: home-delivery orders are
 * assigned to the store whose district / city appears in the shipping address.
 */
public record StoreRequest(

        @NotBlank(message = "Vui lòng nhập tên chi nhánh")
        @Size(max = 120, message = "Tên chi nhánh tối đa 120 ký tự")
        @Schema(example = "POY Quận 1")
        String name,

        @NotBlank(message = "Vui lòng nhập địa chỉ")
        @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
        @Schema(example = "12 Nguyễn Huệ, Phường Bến Nghé")
        String address,

        @NotBlank(message = "Vui lòng nhập quận / huyện")
        @Size(max = 100, message = "Quận / huyện tối đa 100 ký tự")
        @Schema(example = "Quận 1")
        String district,

        @NotBlank(message = "Vui lòng nhập tỉnh / thành phố")
        @Size(max = 100, message = "Tỉnh / thành phố tối đa 100 ký tự")
        @Schema(example = "Hồ Chí Minh")
        String city,

        @Pattern(regexp = "^\\s*([0-9+\\s().-]{8,20})?\\s*$",
                message = "Số điện thoại gồm 8-20 chữ số hoặc ký tự + ( ) . -")
        @Schema(example = "02838123456")
        String phone,

        @Email(message = "Email không hợp lệ")
        @Size(max = 120, message = "Email tối đa 120 ký tự")
        @Schema(example = "q1@poy.vn")
        String email,

        @DecimalMin(value = "-90", message = "Vĩ độ từ -90 đến 90")
        @DecimalMax(value = "90", message = "Vĩ độ từ -90 đến 90")
        @Schema(description = "Optional, for a map later; not used to find the nearest store", example = "10.7769")
        BigDecimal latitude,

        @DecimalMin(value = "-180", message = "Kinh độ từ -180 đến 180")
        @DecimalMax(value = "180", message = "Kinh độ từ -180 đến 180")
        @Schema(example = "106.7009")
        BigDecimal longitude,

        @Schema(description = "false = tạm đóng (not offered for pickup, not assigned new orders); null counts as true",
                example = "true")
        Boolean active
) {
}
