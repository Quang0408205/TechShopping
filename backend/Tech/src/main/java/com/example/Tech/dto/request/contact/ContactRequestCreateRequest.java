package com.example.Tech.dto.request.contact;

import com.example.Tech.entity.contact.ContactTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The Contact form. Texts are trimmed by the service; the message is checked again after trimming.
 */
public record ContactRequestCreateRequest(

        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(min = 2, max = 100, message = "Họ tên từ 2 đến 100 ký tự")
        @Schema(example = "Nguyễn Văn A")
        String fullName,

        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Email chưa hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        @Schema(example = "khach@example.com")
        String email,

        @Pattern(regexp = "^\\s*(0|\\+84)[0-9]{9,10}\\s*$", message = "Số điện thoại chưa hợp lệ (ví dụ 0912345678)")
        @Schema(description = "Optional", example = "0912345678")
        String phone,

        @NotNull(message = "Vui lòng chọn chủ đề")
        @Schema(example = "ORDER")
        ContactTopic topic,

        @NotBlank(message = "Vui lòng nhập nội dung")
        @Size(min = 10, max = 2000, message = "Nội dung từ 10 đến 2000 ký tự")
        @Schema(example = "Tôi muốn hỏi về thời gian giao hàng của đơn #12")
        String message
) {
}
