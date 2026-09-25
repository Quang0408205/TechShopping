package com.example.Tech.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Own customer profile. PUT semantics: omitted fields are cleared. Loyalty points and total spent are read-only.
 */
public record CustomerProfileUpdateRequest(

        @Schema(example = "1995-08-20")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Schema(allowableValues = {"MALE", "FEMALE", "OTHER"})
        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE or OTHER")
        String gender,

        @Schema(example = "123 Nguyễn Huệ")
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Schema(example = "TP. Hồ Chí Minh")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Schema(example = "Quận 1")
        @Size(max = 100, message = "District must be at most 100 characters")
        String district,

        @Schema(example = "Phường Bến Nghé")
        @Size(max = 100, message = "Ward must be at most 100 characters")
        String ward,

        @Size(max = 20, message = "Postal code must be at most 20 characters")
        String postalCode,

        @Size(max = 1000, message = "Default shipping address must be at most 1000 characters")
        String defaultShippingAddress
) {
}
