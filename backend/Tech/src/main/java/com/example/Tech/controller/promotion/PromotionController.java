package com.example.Tech.controller.promotion;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.promotion.FlashSaleResponse;
import com.example.Tech.service.promotion.FlashSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plan v6 B4, public: the promotions as the home page shows them. */
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Running promotions for the home page (public)")
public class PromotionController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/flash-sale")
    @Operation(summary = "Flash sale: up to 8 in-stock products under a running promotion + the soonest end time "
            + "(items empty, endsAt null when nothing is running)")
    public ResponseEntity<ApiResult<FlashSaleResponse>> flashSale() {
        return ResponseEntity.ok(ApiResult.ok(flashSaleService.current()));
    }
}
