package com.example.Tech.service.impl.product;

import com.example.Tech.dto.response.product.FilterGroupResponse;
import com.example.Tech.dto.response.product.FilterGroupResponse.FilterValueResponse;
import com.example.Tech.dto.response.product.NeedResponse;
import com.example.Tech.entity.product.Need;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductFilterValue;
import com.example.Tech.entity.product.ProductNeed;
import com.example.Tech.entity.product.ProductSpecification;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.NeedRepository;
import com.example.Tech.repository.product.ProductFilterValueRepository;
import com.example.Tech.repository.product.ProductNeedRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductSpecificationRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.product.ProductFilterService;
import com.example.Tech.service.product.filter.FilterNormalizer;
import com.example.Tech.service.product.filter.FilterNormalizer.Group;
import com.example.Tech.service.product.filter.FilterNormalizer.Type;
import com.example.Tech.service.product.filter.FilterNormalizer.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFilterServiceImpl implements ProductFilterService {

    private final ProductRepository productRepository;
    private final ProductSpecificationRepository specificationRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductFilterValueRepository filterValueRepository;
    private final NeedRepository needRepository;
    private final ProductNeedRepository productNeedRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void reindex(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        filterValueRepository.deleteAllByProductId(productId);
        if (product == null || product.getDeletedAt() != null) {
            return;
        }
        filterValueRepository.flush();
        saveValues(product);
    }

    @Override
    @Transactional
    public int reindexAll() {
        filterValueRepository.deleteEverything();
        filterValueRepository.flush();
        int count = 0;
        for (Product product : productRepository.findAll()) {
            if (product.getDeletedAt() == null) {
                saveValues(product);
                count++;
            }
        }
        log.info("Rebuilt the filter values of {} products", count);
        return count;
    }

    private void saveValues(Product product) {
        Type type = FilterNormalizer.typeOf(product.getCategory().getSlug());
        if (type == Type.NONE) {
            return;
        }
        Map<String, String> specs = new LinkedHashMap<>();
        for (ProductSpecification spec : specificationRepository.findAllByProductIdOrderBySpecOrderAscIdAsc(product.getId())) {
            if (spec.getVariantId() == null) {
                specs.putIfAbsent(spec.getSpecName(), spec.getSpecValue());
            }
        }
        List<String[]> variants = new ArrayList<>();
        for (ProductVariant variant : variantRepository.findAllByProductIdInOrderByIdAsc(List.of(product.getId()))) {
            variants.add(new String[]{variant.getRam(), variant.getStorage()});
        }
        List<ProductFilterValue> rows = new ArrayList<>();
        for (Value value : FilterNormalizer.normalize(type, product.getName(), specs, variants)) {
            ProductFilterValue row = new ProductFilterValue();
            row.setProductId(product.getId());
            row.setGroupKey(value.group());
            row.setLabel(value.label());
            row.setSortOrder(value.order());
            rows.add(row);
        }
        filterValueRepository.saveAll(rows);
    }

    @Override
    public List<FilterGroupResponse> getFilters(Integer categoryId) {
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, categoryId));
        Map<String, List<Object[]>> byGroup = new LinkedHashMap<>();
        for (Object[] row : filterValueRepository.countByCategory(categoryId)) {
            byGroup.computeIfAbsent((String) row[0], key -> new ArrayList<>()).add(row);
        }
        List<FilterGroupResponse> result = new ArrayList<>();
        for (Group group : FilterNormalizer.groupsOf(FilterNormalizer.typeOf(category.getSlug()))) {
            List<Object[]> rows = byGroup.getOrDefault(group.key(), List.of());
            List<FilterValueResponse> values = rows.stream()
                    .sorted(Comparator.comparingInt((Object[] r) -> ((Number) r[2]).intValue()).thenComparing(r -> (String) r[1]))
                    .map(r -> new FilterValueResponse((String) r[1], (String) r[1], ((Number) r[3]).longValue()))
                    .toList();
            // a group with a single value cannot narrow anything (e.g. every smart watch is "Unisex")
            if (values.size() >= 2) {
                result.add(new FilterGroupResponse(group.key(), group.title(), values));
            }
        }
        List<NeedResponse> needs = getNeeds(categoryId).stream().filter(n -> n.productCount() > 0).toList();
        if (!needs.isEmpty()) {
            result.add(0, new FilterGroupResponse("need", "Nhu cầu",
                    needs.stream().map(n -> new FilterValueResponse(n.slug(), n.name(), n.productCount())).toList()));
        }
        return result;
    }

    @Override
    public List<NeedResponse> getNeeds(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, categoryId);
        }
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (Object[] row : productNeedRepository.countByCategory(categoryId)) {
            counts.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return needRepository.findAllByCategoryIdOrderByDisplayOrderAscIdAsc(categoryId).stream()
                .map(n -> toResponse(n, counts.getOrDefault(n.getId(), 0L)))
                .toList();
    }

    @Override
    public List<NeedResponse> getProductNeeds(Long productId) {
        findProduct(productId);
        return productNeedRepository.findAllByProductId(productId).stream()
                .map(ProductNeed::getNeed)
                .sorted(Comparator.comparingInt(Need::getDisplayOrder).thenComparing(Need::getId))
                .map(n -> toResponse(n, 0))
                .toList();
    }

    @Override
    @Transactional
    public List<NeedResponse> setProductNeeds(Long productId, List<Integer> needIds) {
        Product product = findProduct(productId);
        Set<Integer> wanted = new LinkedHashSet<>(needIds == null ? List.of() : needIds);
        List<Need> needs = new ArrayList<>();
        for (Integer id : wanted) {
            Need need = needRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_DATA, "Nhu cầu %d không tồn tại".formatted(id)));
            if (!need.getCategoryId().equals(product.getCategory().getId())) {
                throw new BusinessException(ErrorCode.INVALID_PRODUCT_DATA,
                        "Nhu cầu \"%s\" không thuộc danh mục của sản phẩm".formatted(need.getName()));
            }
            needs.add(need);
        }
        productNeedRepository.deleteAllByProductId(productId);
        productNeedRepository.flush();
        List<ProductNeed> rows = new ArrayList<>();
        for (Need need : needs) {
            ProductNeed row = new ProductNeed();
            row.setProductId(productId);
            row.setNeed(need);
            rows.add(row);
        }
        productNeedRepository.saveAll(rows);
        return needs.stream().sorted(Comparator.comparingInt(Need::getDisplayOrder).thenComparing(Need::getId))
                .map(n -> toResponse(n, 0)).toList();
    }

    private NeedResponse toResponse(Need need, long count) {
        return new NeedResponse(need.getId(), need.getCategoryId(), need.getSlug(), need.getName(), count);
    }

    private Product findProduct(Long productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));
    }
}
