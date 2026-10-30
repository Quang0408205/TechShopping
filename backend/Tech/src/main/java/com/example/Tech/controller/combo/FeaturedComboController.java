package com.example.Tech.controller.combo;

import com.example.Tech.dto.response.combo.FeaturedComboResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.service.combo.ComboQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public: the combos the home page features. */
@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
@Tag(name = "Combos", description = "Buy-together combos on a product page (public)")
public class FeaturedComboController {

    static final int FEATURED = 3;

    private final ComboQueryService comboQueryService;

    @GetMapping("/featured")
    @Operation(summary = "Up to 3 running combos (newest first) with the basket price before / after the discount; empty list when none")
    public ResponseEntity<ApiResult<List<FeaturedComboResponse>>> featured() {
        return ResponseEntity.ok(ApiResult.ok(comboQueryService.featured(FEATURED)));
    }
}
