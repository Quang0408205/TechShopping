package com.example.Tech.dto.request.product;

import com.example.Tech.util.SlugUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must be at most 100 characters")
        String name,

        @Schema(description = "Generated from name when omitted", example = "dien-thoai")
        @Size(max = 100, message = "Slug must be at most 100 characters")
        @Pattern(regexp = SlugUtil.SLUG_REGEX, message = "Slug must contain only lowercase letters, digits and dashes")
        String slug,

        String description,

        @Schema(description = "Parent category id; null for a root category")
        Integer parentId,

        String iconUrl,

        @PositiveOrZero(message = "Display order must not be negative")
        Integer displayOrder,

        @Schema(description = "Unchanged when omitted")
        Boolean isActive
) {
}
