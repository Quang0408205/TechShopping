package com.example.Tech.dto.request.auth;

import com.example.Tech.util.AccountUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration. The account always gets the CUSTOMER role.
 */
public record RegisterRequest(

        @Schema(description = "Stored in lower case", example = "an.nguyen@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        @Size(max = 120, message = "Email must be at most 120 characters")
        String email,

        @Schema(description = "Letters, digits, dot, underscore and dash; stored in lower case", example = "an.nguyen")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3 to 50 characters")
        @Pattern(regexp = AccountUtil.USERNAME_REGEX,
                message = "Username may contain only letters, digits, dot, underscore and dash")
        String username,

        @Schema(description = "At least 8 characters", example = "Matkhau@123")
        @NotBlank(message = "Password is required")
        @Size(min = AccountUtil.MIN_PASSWORD_LENGTH, max = AccountUtil.MAX_PASSWORD_BYTES,
                message = "Password must be 8 to 72 characters")
        String password,

        @Schema(example = "Nguyễn Văn An")
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullname,

        @Schema(example = "0901234567")
        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone
) {
}
