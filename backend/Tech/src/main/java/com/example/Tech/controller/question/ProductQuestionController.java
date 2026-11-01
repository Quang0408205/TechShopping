package com.example.Tech.controller.question;

import com.example.Tech.dto.request.question.AskQuestionRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.question.ProductQuestionResponse;
import com.example.Tech.service.question.ProductQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Product questions", description = "Questions about a product: public once answered; a signed-in customer asks")
public class ProductQuestionController {

    private final ProductQuestionService questionService;

    @GetMapping("/products/{productId}/questions")
    @Operation(summary = "Answered, visible questions of a product, newest answer first (public)")
    @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    public ResponseEntity<ApiResult<PageResponse<ProductQuestionResponse>>> answered(@PathVariable Long productId,
                                                                                    @RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResult.ok(questionService.answered(productId, page, size)));
    }

    @PostMapping("/product-questions")
    @Operation(summary = "Ask a question (10-500 characters, at most 5 a day); it shows publicly once staff answer it")
    @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    @ApiResponse(responseCode = "429", description = "QUESTION_LIMIT_REACHED")
    public ResponseEntity<ApiResult<Void>> ask(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody AskQuestionRequest request) {
        questionService.ask(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(null));
    }
}
