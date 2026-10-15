package com.example.Tech.controller.upload;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.upload.UploadedImageResponse;
import com.example.Tech.service.upload.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload first, then send the returned URL as imageUrl (POST /products images, POST /product-images).
 * Covered by the /api/v1/admin/** rule (ADMIN).
 */
@RestController
@RequestMapping("/api/v1/admin/uploads")
@RequiredArgsConstructor
@Tag(name = "Uploads", description = "Image uploads (ADMIN)")
public class ImageUploadController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/product-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a product image (JPEG, PNG or WebP, at most 5 MB) and get its public URL")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Image stored"),
            @ApiResponse(responseCode = "400", description = "INVALID_IMAGE_FILE or VALIDATION_ERROR (no file part)"),
            @ApiResponse(responseCode = "413", description = "IMAGE_TOO_LARGE")
    })
    public ResponseEntity<ApiResult<UploadedImageResponse>> uploadProductImage(@RequestPart("file") MultipartFile file) {
        String url = imageStorageService.storeProductImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(new UploadedImageResponse(url)));
    }
}
