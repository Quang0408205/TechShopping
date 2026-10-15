package com.example.Tech.controller.store;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.store.PublicStoreResponse;
import com.example.Tech.service.store.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public list of open stores (no login needed), used by the checkout "nhận tại chi nhánh" picker.
 */
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Open stores (public)")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    @Operation(summary = "List open stores, by name")
    @ApiResponse(responseCode = "200", description = "Open stores")
    public ResponseEntity<ApiResult<List<PublicStoreResponse>>> listOpen() {
        return ResponseEntity.ok(ApiResult.ok(storeService.listOpen()));
    }
}
