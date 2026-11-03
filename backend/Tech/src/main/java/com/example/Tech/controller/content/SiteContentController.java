package com.example.Tech.controller.content;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.content.ContentPageResponse;
import com.example.Tech.service.content.SiteContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** What visitors see: the published static pages. No token needed. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Content", description = "Static pages (public reads)")
public class SiteContentController {

    private final SiteContentService contentService;

    @GetMapping("/pages")
    @Operation(summary = "Published pages (slug and title only)")
    public ResponseEntity<ApiResult<List<ContentPageResponse>>> pages() {
        return ResponseEntity.ok(ApiResult.ok(contentService.publishedPages()));
    }

    @GetMapping("/pages/{slug}")
    @Operation(summary = "One published page with its (already filtered) HTML body")
    @ApiResponse(responseCode = "404", description = "CONTENT_PAGE_NOT_FOUND (also for an unpublished page)")
    public ResponseEntity<ApiResult<ContentPageResponse>> page(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResult.ok(contentService.publishedPage(slug)));
    }
}
