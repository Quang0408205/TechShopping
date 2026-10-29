package com.example.Tech.controller.review;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.review.ReviewHighlightResponse;
import com.example.Tech.service.review.ReviewHighlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Plan v6 B5, public: the review block of the home page. */
@RestController
@RequestMapping("/api/v1/reviews/highlights")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews: public reads, the author's own writes")
public class ReviewHighlightController {

    static final int SIZE = 6;

    private final ReviewHighlightService reviewHighlightService;

    @GetMapping
    @Operation(summary = "Up to 6 newest visible 4-5 star reviews of products on sale (empty list when none)")
    public ResponseEntity<ApiResult<List<ReviewHighlightResponse>>> latest() {
        return ResponseEntity.ok(ApiResult.ok(reviewHighlightService.latest(SIZE)));
    }
}
