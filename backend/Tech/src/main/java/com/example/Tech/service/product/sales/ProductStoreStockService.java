package com.example.Tech.service.product.sales;

import com.example.Tech.dto.response.product.ProductStoreStockResponse;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** "Còn hàng tại cửa hàng nào" on the product page, from the stock of the open stores. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductStoreStockService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public List<ProductStoreStockResponse> stockByStore(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return inventoryRepository.findOpenStoreRowsByProductId(productId).stream()
                .filter(row -> row.getQuantity() > 0)
                .map(row -> new ProductStoreStockResponse(row.getId().getVariantId(), row.getStore().getId(),
                        row.getStore().getName(), row.getStore().getAddress(), row.getStore().getDistrict(),
                        row.getStore().getCity(), row.getQuantity()))
                .sorted(Comparator.comparing(ProductStoreStockResponse::storeName))
                .toList();
    }
}
