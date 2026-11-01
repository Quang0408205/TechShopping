package com.example.Tech.controller.content;

import com.example.Tech.dto.request.content.BannerRequest;
import com.example.Tech.dto.request.content.ContentPageRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.content.BannerResponse;
import com.example.Tech.dto.response.content.ContentPageResponse;
import com.example.Tech.service.content.SiteContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Writing banners and static pages. ADMIN only (URL rule, @PreAuthorize and the service). */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Content", description = "Home banners and static pages")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
@ApiResponse(responseCode = "403", description = "ACCESS_DENIED")
public class AdminSiteContentController {

    private final SiteContentService contentService;

    @GetMapping("/banners")
    @Operation(summary = "Every banner, including inactive and expired ones")
    public ResponseEntity<ApiResult<List<BannerResponse>>> banners(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(contentService.listBanners(userId(jwt))));
    }

    @PostMapping("/banners")
    @Operation(summary = "Add a banner")
    public ResponseEntity<ApiResult<BannerResponse>> createBanner(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                  @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(contentService.createBanner(userId(jwt), request)));
    }

    @PutMapping("/banners/{id}")
    @Operation(summary = "Replace a banner")
    @ApiResponse(responseCode = "404", description = "BANNER_NOT_FOUND")
    public ResponseEntity<ApiResult<BannerResponse>> updateBanner(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable Integer id,
                                                                  @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(ApiResult.ok(contentService.updateBanner(userId(jwt), id, request)));
    }

    @DeleteMapping("/banners/{id}")
    @Operation(summary = "Delete a banner")
    @ApiResponse(responseCode = "404", description = "BANNER_NOT_FOUND")
    public ResponseEntity<ApiResult<Void>> deleteBanner(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable Integer id) {
        contentService.deleteBanner(userId(jwt), id);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @GetMapping("/pages")
    @Operation(summary = "Every page (titles), published or not")
    public ResponseEntity<ApiResult<List<ContentPageResponse>>> pages(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(contentService.listPages(userId(jwt))));
    }

    @GetMapping("/pages/{slug}")
    @Operation(summary = "One page with its stored HTML")
    @ApiResponse(responseCode = "404", description = "CONTENT_PAGE_NOT_FOUND")
    public ResponseEntity<ApiResult<ContentPageResponse>> page(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable String slug) {
        return ResponseEntity.ok(ApiResult.ok(contentService.getPage(userId(jwt), slug)));
    }

    @PutMapping("/pages/{slug}")
    @Operation(summary = "Create or replace a page; the HTML is filtered to a short list of formatting tags",
            description = "Allowed: p, br, hr, strong, b, em, i, u, ul, ol, li, h2-h4, blockquote, a (http / https / mailto / tel / "
                    + "same-site path), img (http / https / /uploads), table parts. Scripts, styles and event attributes are removed.")
    public ResponseEntity<ApiResult<ContentPageResponse>> savePage(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                   @PathVariable String slug,
                                                                   @Valid @RequestBody ContentPageRequest request) {
        return ResponseEntity.ok(ApiResult.ok(contentService.savePage(userId(jwt), slug, request)));
    }

    @DeleteMapping("/pages/{slug}")
    @Operation(summary = "Delete a page")
    @ApiResponse(responseCode = "404", description = "CONTENT_PAGE_NOT_FOUND")
    public ResponseEntity<ApiResult<Void>> deletePage(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable String slug) {
        contentService.deletePage(userId(jwt), slug);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
