package com.example.Tech.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ProductSuggestResult(
        List<ProductSuggestionResponse> items,
        @Schema(description = "Number of products on sale matching the query (for \"Xem tất cả kết quả\")")
        long total
) {
}
