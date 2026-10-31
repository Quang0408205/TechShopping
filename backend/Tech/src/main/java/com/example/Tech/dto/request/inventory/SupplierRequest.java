package com.example.Tech.dto.request.inventory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code active} null on create means active; null on update means unchanged. */
public record SupplierRequest(

        @NotBlank(message = "Vui lòng nhập tên nhà cung cấp")
        @Size(max = 150, message = "Tên nhà cung cấp tối đa 150 ký tự")
        String name,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phone,

        @Email(message = "Email không hợp lệ")
        @Size(max = 120, message = "Email tối đa 120 ký tự")
        String email,

        @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
        String address,

        @Size(max = 20, message = "Mã số thuế tối đa 20 ký tự")
        String taxCode,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note,

        Boolean active
) {
}
