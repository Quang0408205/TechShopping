package com.example.Tech.service.combo;

import com.example.Tech.dto.request.combo.ComboRequest;
import com.example.Tech.dto.response.combo.ComboResponse;
import com.example.Tech.dto.response.combo.ComboSuggestionResponse;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.combo.Combo;
import com.example.Tech.entity.combo.ComboDiscountType;
import com.example.Tech.entity.combo.ComboItem;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.combo.ComboRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Plan v5 GĐ10: combo management, ADMIN only (re-checked in the database on every call). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminComboService {

    private final ComboRepository comboRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CurrentUserLoader currentUserLoader;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public PageResponse<ComboResponse> search(Long adminId, String keyword, String status, Pageable pageable) {
        admin(adminId);
        LocalDateTime now = LocalDateTime.now(clock);
        var page = comboRepository.findAll(matching(keyword, status, now), pageable);
        return PageResponse.from(new org.springframework.data.domain.PageImpl<>(toResponses(page.getContent()), pageable,
                page.getTotalElements()));
    }

    public ComboResponse getById(Long adminId, Long id) {
        admin(adminId);
        return toResponses(List.of(find(id))).get(0);
    }

    @Transactional
    public ComboResponse create(Long adminId, ComboRequest request) {
        User admin = admin(adminId);
        Combo combo = new Combo();
        combo.setCreatedBy(admin);
        apply(combo, request);
        Combo saved = comboRepository.saveAndFlush(combo);
        log.info("Admin id={} created combo '{}' (id={})", adminId, saved.getName(), saved.getId());
        return toResponses(List.of(saved)).get(0);
    }

    @Transactional
    public ComboResponse update(Long adminId, Long id, ComboRequest request) {
        admin(adminId);
        Combo combo = find(id);
        apply(combo, request);
        Combo saved = comboRepository.saveAndFlush(combo);
        log.info("Admin id={} updated combo '{}' (id={})", adminId, saved.getName(), id);
        return toResponses(List.of(saved)).get(0);
    }

    @Transactional
    public ComboResponse setActive(Long adminId, Long id, boolean active) {
        admin(adminId);
        Combo combo = find(id);
        combo.setActive(active);
        log.info("Admin id={} switched combo id={} {}", adminId, id, active ? "on" : "off");
        return toResponses(List.of(comboRepository.saveAndFlush(combo))).get(0);
    }

    /** Only a combo no order used; otherwise switch it off. */
    @Transactional
    public void delete(Long adminId, Long id) {
        admin(adminId);
        Combo combo = find(id);
        if (comboRepository.usedByOrders(id)) {
            throw new BusinessException(ErrorCode.COMBO_IN_USE);
        }
        comboRepository.delete(combo);
        log.info("Admin id={} deleted combo '{}' (id={})", adminId, combo.getName(), id);
    }

    /** Add-on ideas for a main product: the AI "bought together" relations, strongest first. */
    public List<ComboSuggestionResponse> suggestions(Long adminId, Long productId, int limit) {
        admin(adminId);
        productRepository.findById(productId).filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.invalidField("productId", "Sản phẩm không tồn tại"));
        List<Object[]> rows = jdbc.query("""
                select r.related_product_id, r.relation_type, max(r.strength) strength
                from product_relations r join products p on p.product_id = r.related_product_id
                where r.product_id = ? and p.deleted_at is null and coalesce(p.is_active, true)
                group by r.related_product_id, r.relation_type
                order by max(r.strength) desc nulls last, r.related_product_id
                limit ?
                """, (rs, i) -> new Object[] {rs.getLong(1), rs.getString(2), rs.getBigDecimal(3)}, productId, limit * 3);
        Map<Long, Object[]> best = new java.util.LinkedHashMap<>();
        rows.forEach(row -> best.putIfAbsent((Long) row[0], row));
        List<Long> ids = best.keySet().stream().limit(limit).toList();
        Map<Long, Product> products = new HashMap<>();
        productRepository.findAllById(ids).forEach(p -> products.put(p.getId(), p));
        Map<Long, String> images = images(ids);
        Map<Long, BigDecimal> prices = minPrices(ids);
        List<ComboSuggestionResponse> result = new ArrayList<>();
        for (Long id : ids) {
            Product product = products.get(id);
            if (product != null) {
                Object[] row = best.get(id);
                result.add(new ComboSuggestionResponse(id, product.getName(), product.getSlug(), images.get(id),
                        prices.get(id), (String) row[1], (BigDecimal) row[2]));
            }
        }
        return result;
    }

    private void apply(Combo combo, ComboRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw BusinessException.invalidField("endsAt", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        Product main = productRepository.findById(request.mainProductId()).filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.invalidField("mainProductId", "Sản phẩm chính không tồn tại"));
        Set<Long> seen = new HashSet<>();
        for (ComboRequest.Item item : request.items()) {
            if (!seen.add(item.productId())) {
                throw BusinessException.invalidField("items", "Một sản phẩm chỉ được thêm một lần trong combo");
            }
            if (item.productId().equals(main.getId())) {
                throw BusinessException.invalidField("items", "Món mua kèm không được trùng sản phẩm chính");
            }
            if (item.discountType() == ComboDiscountType.PERCENT && item.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw BusinessException.invalidField("items", "Giảm theo % tối đa 100");
            }
        }
        Map<Long, Product> addOns = new HashMap<>();
        productRepository.findAllById(seen).stream().filter(p -> p.getDeletedAt() == null)
                .forEach(p -> addOns.put(p.getId(), p));
        if (addOns.size() != seen.size()) {
            throw BusinessException.invalidField("items", "Có sản phẩm mua kèm không tồn tại");
        }
        combo.setName(request.name().trim());
        combo.setDescription(request.description() == null || request.description().isBlank() ? null
                : request.description().trim());
        combo.setMainProduct(main);
        combo.setStartsAt(request.startsAt());
        combo.setEndsAt(request.endsAt());
        combo.setActive(request.active() == null || request.active());

        // change in place: delete + insert of the same key in one flush would hit the primary key
        combo.getItems().removeIf(existing -> !seen.contains(existing.getProduct().getId()));
        Map<Long, ComboItem> current = new HashMap<>();
        combo.getItems().forEach(existing -> current.put(existing.getProduct().getId(), existing));
        for (ComboRequest.Item item : request.items()) {
            ComboItem target = current.get(item.productId());
            if (target == null) {
                target = new ComboItem();
                target.setCombo(combo);
                target.setProduct(addOns.get(item.productId()));
                combo.getItems().add(target);
            }
            target.setDiscountType(item.discountType());
            target.setDiscountValue(item.discountValue());
        }
    }

    private Combo find(Long id) {
        return comboRepository.findWithItems(id).orElseThrow(() -> new BusinessException(ErrorCode.COMBO_NOT_FOUND));
    }

    private User admin(Long adminId) {
        return currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
    }

    static String status(Combo combo, LocalDateTime now) {
        if (!Boolean.TRUE.equals(combo.getActive())) {
            return "PAUSED";
        }
        if (now.isBefore(combo.getStartsAt())) {
            return "UPCOMING";
        }
        return now.isBefore(combo.getEndsAt()) ? "RUNNING" : "ENDED";
    }

    private List<ComboResponse> toResponses(Collection<Combo> combos) {
        LocalDateTime now = LocalDateTime.now(clock);
        Set<Long> productIds = new HashSet<>();
        for (Combo combo : combos) {
            productIds.add(combo.getMainProduct().getId());
            combo.getItems().forEach(item -> productIds.add(item.getProduct().getId()));
        }
        Map<Long, String> images = images(productIds);
        Map<Long, BigDecimal> prices = minPrices(productIds);
        Map<Long, Long> orderCounts = new HashMap<>();
        if (!combos.isEmpty()) {
            String in = String.join(",", combos.stream().map(c -> "?").toList());
            jdbc.query("select combo_id, count(distinct order_id) from order_combos where combo_id in (" + in + ") group by combo_id",
                    rs -> {
                        orderCounts.put(rs.getLong(1), rs.getLong(2));
                    }, combos.stream().map(Combo::getId).toArray());
        }
        List<ComboResponse> result = new ArrayList<>();
        for (Combo combo : combos) {
            long orders = orderCounts.getOrDefault(combo.getId(), 0L);
            List<ComboResponse.Item> items = combo.getItems().stream()
                    .sorted(java.util.Comparator.comparing(item -> item.getProduct().getId()))
                    .map(item -> new ComboResponse.Item(ref(item.getProduct(), images, prices),
                            item.getDiscountType().name(), item.getDiscountValue()))
                    .toList();
            result.add(new ComboResponse(combo.getId(), combo.getName(), combo.getDescription(),
                    ref(combo.getMainProduct(), images, prices), items, combo.getStartsAt(), combo.getEndsAt(),
                    Boolean.TRUE.equals(combo.getActive()), status(combo, now), orders, orders == 0,
                    combo.getCreatedAt(), combo.getUpdatedAt()));
        }
        return result;
    }

    private static ComboResponse.ProductRef ref(Product product, Map<Long, String> images, Map<Long, BigDecimal> prices) {
        return new ComboResponse.ProductRef(product.getId(), product.getName(), product.getSlug(),
                images.get(product.getId()), prices.get(product.getId()));
    }

    private Map<Long, String> images(Collection<Long> productIds) {
        Map<Long, String> images = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (ProductImage image : imageRepository.findAllByProductIdInBestFirst(productIds)) {
                images.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
            }
        }
        return images;
    }

    private Map<Long, BigDecimal> minPrices(Collection<Long> productIds) {
        Map<Long, BigDecimal> prices = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (ProductVariant variant : variantRepository.findAllByProductIdInOrderByIdAsc(productIds)) {
                prices.merge(variant.getProduct().getId(), variant.getPrice(), BigDecimal::min);
            }
        }
        return prices;
    }

    private static Specification<Combo> matching(String keyword, String status, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), like, '\\'));
            }
            if (status != null && !status.isBlank()) {
                switch (status) {
                    case "PAUSED" -> predicates.add(cb.isFalse(root.get("active")));
                    case "UPCOMING" -> predicates.add(cb.and(cb.isTrue(root.get("active")),
                            cb.greaterThan(root.get("startsAt"), now)));
                    case "ENDED" -> predicates.add(cb.and(cb.isTrue(root.get("active")),
                            cb.lessThanOrEqualTo(root.get("endsAt"), now)));
                    default -> predicates.add(cb.and(cb.isTrue(root.get("active")),
                            cb.lessThanOrEqualTo(root.get("startsAt"), now), cb.greaterThan(root.get("endsAt"), now)));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
