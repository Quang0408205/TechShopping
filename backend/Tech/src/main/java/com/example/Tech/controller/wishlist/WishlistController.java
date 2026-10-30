package com.example.Tech.controller.wishlist;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.wishlist.PriceAlertRunResponse;
import com.example.Tech.dto.response.wishlist.WishlistItemResponse;
import com.example.Tech.service.wishlist.WishlistPriceAlertService;
import com.example.Tech.service.wishlist.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The logged-in customer's wishlist, and the ADMIN trigger of the price-drop check. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Saved (favourite) products and price-drop emails")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
public class WishlistController {

    private final WishlistService wishlistService;
    private final WishlistPriceAlertService priceAlertService;

    @GetMapping("/api/v1/wishlist")
    @Operation(summary = "My saved products, newest first, with the price now and the price when saved")
    public ResponseEntity<ApiResult<List<WishlistItemResponse>>> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(wishlistService.list(userId(jwt))));
    }

    @GetMapping("/api/v1/wishlist/ids")
    @Operation(summary = "Ids of my saved products (to colour the hearts on the cards)")
    public ResponseEntity<ApiResult<List<Long>>> ids(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(wishlistService.ids(userId(jwt))));
    }

    @PostMapping("/api/v1/wishlist/{productId}")
    @Operation(summary = "Save a product (saving it again changes nothing)")
    @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND (also a product taken off sale)")
    public ResponseEntity<ApiResult<Void>> add(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                               @PathVariable Long productId) {
        wishlistService.add(userId(jwt), productId);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @DeleteMapping("/api/v1/wishlist/{productId}")
    @Operation(summary = "Remove a product from my wishlist (removing a product that is not there changes nothing)")
    public ResponseEntity<ApiResult<Void>> remove(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long productId) {
        wishlistService.remove(userId(jwt), productId);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @PostMapping("/api/v1/admin/wishlist/price-alerts/run")
    @Operation(summary = "ADMIN: run the price-drop check now (it also runs every hour)")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN (not ADMIN)")
    public ResponseEntity<ApiResult<PriceAlertRunResponse>> runPriceAlerts() {
        return ResponseEntity.ok(ApiResult.ok(priceAlertService.run()));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
