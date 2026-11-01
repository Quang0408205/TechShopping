package com.example.Tech.controller.question;

import com.example.Tech.dto.request.question.AnswerQuestionRequest;
import com.example.Tech.dto.request.question.HideQuestionRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.question.AdminQuestionResponse;
import com.example.Tech.service.question.ProductQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Answering and hiding product questions: STAFF, BRANCH_MANAGER and ADMIN (the questions belong to no store). */
@RestController
@RequestMapping("/api/v1/admin/questions")
@RequiredArgsConstructor
@Tag(name = "Admin - Product questions", description = "Answer or hide customer questions")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
@ApiResponse(responseCode = "403", description = "ACCESS_DENIED")
public class AdminProductQuestionController {

    private final ProductQuestionService questionService;

    @GetMapping
    @Operation(summary = "Questions, newest first", description = "status = PENDING | ANSWERED | HIDDEN (empty = all); "
            + "keyword matches the question, product, asker name or email")
    public ResponseEntity<ApiResult<PageResponse<AdminQuestionResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResult.ok(questionService.adminList(Long.valueOf(jwt.getSubject()), status, keyword, page, size)));
    }

    @PutMapping("/{id}/answer")
    @Operation(summary = "Answer (or change the answer of) a question; it becomes public unless hidden")
    @ApiResponse(responseCode = "404", description = "PRODUCT_QUESTION_NOT_FOUND")
    public ResponseEntity<ApiResult<AdminQuestionResponse>> answer(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                   @PathVariable Long id,
                                                                   @Valid @RequestBody AnswerQuestionRequest request) {
        return ResponseEntity.ok(ApiResult.ok(questionService.answer(Long.valueOf(jwt.getSubject()), id, request)));
    }

    @PatchMapping("/{id}/hidden")
    @Operation(summary = "Hide or show a question on the product page")
    @ApiResponse(responseCode = "404", description = "PRODUCT_QUESTION_NOT_FOUND")
    public ResponseEntity<ApiResult<AdminQuestionResponse>> hide(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody HideQuestionRequest request) {
        return ResponseEntity.ok(ApiResult.ok(questionService.hide(Long.valueOf(jwt.getSubject()), id, request.hidden())));
    }
}
