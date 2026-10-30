package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.ProductCreateRequest;
import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.product.ProductUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.product.ProductSalesResponse;
import com.example.Tech.dto.response.product.ProductSuggestResult;
import com.example.Tech.service.product.sales.ProductSalesService;
import com.example.Tech.service.product.sales.ProductStoreStockService;
import com.example.Tech.dto.response.product.ProductStoreStockResponse;
import java.util.ArrayList;
import java.util.List;
import com.example.Tech.service.product.ProductService;
import com.example.Tech.service.product.SearchRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import com.example.Tech.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management")
public class ProductController {

    private final ProductService productService;
    private final SearchRateLimiter searchRateLimiter;
    private final ProductSalesService productSalesService;
    private final ProductStoreStockService productStoreStockService;

    @GetMapping
    @Operation(summary = "Search products with pagination (soft-deleted products are excluded)",
            description = "Filters are optional and combined with AND. Sort example: sort=basePrice,desc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of products returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / MALFORMED_REQUEST (e.g. unknown sort field)")
    })
    public ResponseEntity<ApiResult<PageResponse<ProductResponse>>> search(
            @Valid @ParameterObject ProductSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(productService.search(filter, pageable)));
    }

    @GetMapping("/sales")
    @Operation(summary = "Units sold (orders not cancelled) and the best-seller flag of up to 60 products",
            description = "ids = comma separated product ids, as listed on one page of cards")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (ids missing, not numbers or more than 60)")
    public ResponseEntity<ApiResult<List<ProductSalesResponse>>> sales(@RequestParam(required = false) String ids) {
        List<Long> parsed = new ArrayList<>();
        for (String part : ids == null ? new String[0] : ids.split(",")) {
            if (!part.isBlank()) {
                if (!part.trim().matches("\\d{1,18}")) {
                    throw BusinessException.invalidField("ids", "ids must be product ids separated by commas");
                }
                parsed.add(Long.valueOf(part.trim()));
            }
        }
        if (parsed.isEmpty() || parsed.size() > ProductSalesService.MAX_IDS) {
            throw BusinessException.invalidField("ids", "Give 1-%d product ids".formatted(ProductSalesService.MAX_IDS));
        }
        return ResponseEntity.ok(ApiResult.ok(productSalesService.sales(parsed)));
    }

    @GetMapping("/{id}/store-stock")
    @Operation(summary = "Which open stores hold each variant of the product, and how many")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rows for stores with stock (may be empty)"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<List<ProductStoreStockResponse>>> storeStock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(productStoreStockService.stockByStore(id)));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Search box suggestions: products on sale with image and price",
            description = "Every word of q must be in the name, the slug (no accents needed) or the brand. "
                    + "In stock first, then names starting with the first word, then shorter names.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "At most size suggestions + the total number of matches"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (q blank / longer than 100, size not 1-10)"),
            @ApiResponse(responseCode = "429", description = "TOO_MANY_REQUESTS (per-IP limit)")
    })
    public ResponseEntity<ApiResult<ProductSuggestResult>> suggest(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "6") int size,
            HttpServletRequest httpRequest) {
        if (q == null || q.isBlank() || q.trim().length() > 100) {
            throw BusinessException.invalidField("q", "q is required (at most 100 characters)");
        }
        if (size < 1 || size > 10) {
            throw BusinessException.invalidField("size", "size must be 1-10");
        }
        searchRateLimiter.acquire(httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResult.ok(productService.suggest(q, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(productService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_PRODUCT_DATA"),
            @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND / BRAND_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_PRODUCT")
    })
    public ResponseEntity<ApiResult<ProductResponse>> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(productService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_PRODUCT_DATA"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND / CATEGORY_NOT_FOUND / BRAND_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_PRODUCT")
    })
    public ResponseEntity<ApiResult<ProductResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
