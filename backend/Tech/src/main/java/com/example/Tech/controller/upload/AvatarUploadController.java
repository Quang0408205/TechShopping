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
 * Account avatar: upload first, then save the returned URL with PUT /api/v1/users/me (avatarUrl).
 * Any logged-in account (the authenticated /api/v1/** rule).
 */
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Uploads", description = "Image uploads")
public class AvatarUploadController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an avatar (JPEG, PNG or WebP, at most 2 MB) and get its public URL")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Image stored"),
            @ApiResponse(responseCode = "400", description = "INVALID_IMAGE_FILE or VALIDATION_ERROR (no file part)"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
            @ApiResponse(responseCode = "413", description = "IMAGE_TOO_LARGE")
    })
    public ResponseEntity<ApiResult<UploadedImageResponse>> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String url = imageStorageService.storeAvatar(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(new UploadedImageResponse(url)));
    }
}
