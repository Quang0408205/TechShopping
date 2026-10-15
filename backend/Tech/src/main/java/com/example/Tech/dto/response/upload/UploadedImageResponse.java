package com.example.Tech.dto.response.upload;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadedImageResponse(

        @Schema(description = "Public URL to send as imageUrl when creating the product or its images",
                example = "http://localhost:8080/uploads/products/3f2b6c1e-8a4d-4c55-9f0e-2d7b1a9c4e10.jpg")
        String url
) {
}
