package com.example.Tech.service.voucher;

import com.example.Tech.dto.request.voucher.VoucherRequest;
import com.example.Tech.dto.request.voucher.VoucherSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.voucher.VoucherResponse;
import com.example.Tech.dto.response.voucher.VoucherUsageResponse;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.voucher.Voucher;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.voucher.VoucherRepository;
import com.example.Tech.repository.voucher.VoucherUsageRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Plan v5 GĐ4: voucher management, ADMIN only (re-checked in the database on every call). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVoucherService {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_-]{3,30}$");

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository usageRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserLoader currentUserLoader;
    private final Clock clock;

    public PageResponse<VoucherResponse> search(Long adminId, VoucherSearchRequest filter, Pageable pageable) {
        admin(adminId);
        LocalDateTime now = LocalDateTime.now(clock);
        return PageResponse.from(voucherRepository.findAll(matching(filter, now), pageable)
                .map(voucher -> toResponse(voucher, now)));
    }

    public VoucherResponse getById(Long adminId, Long id) {
        admin(adminId);
        return toResponse(find(id), LocalDateTime.now(clock));
    }

    @Transactional
    public VoucherResponse create(Long adminId, VoucherRequest request) {
        User admin = admin(adminId);
        String code = checkedCode(request.code());
        if (voucherRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VOUCHER, ErrorCode.DUPLICATE_VOUCHER.getDefaultMessage(),
                    java.util.Map.of("code", "Mã voucher đã tồn tại"));
        }
        Voucher voucher = new Voucher();
        voucher.setCreatedBy(admin);
        voucher.setCode(code);
        apply(voucher, request);
        Voucher saved = voucherRepository.saveAndFlush(voucher);
        log.info("Admin id={} created voucher {} (id={})", adminId, code, saved.getId());
        return toResponse(saved, LocalDateTime.now(clock));
    }

    /** Replaces every field; the code cannot change once an order used the voucher, nor the limit go below the uses. */
    @Transactional
    public VoucherResponse update(Long adminId, Long id, VoucherRequest request) {
        admin(adminId);
        Voucher voucher = voucherRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));
        String code = checkedCode(request.code());
        if (!code.equals(voucher.getCode())) {
            if (usageRepository.existsByVoucherId(id)) {
                throw new BusinessException(ErrorCode.VOUCHER_IN_USE, "Voucher đã có đơn hàng sử dụng, không đổi được mã",
                        java.util.Map.of("code", "Voucher đã có đơn hàng sử dụng, không đổi được mã"));
            }
            if (voucherRepository.existsByCode(code)) {
                throw new BusinessException(ErrorCode.DUPLICATE_VOUCHER, ErrorCode.DUPLICATE_VOUCHER.getDefaultMessage(),
                        java.util.Map.of("code", "Mã voucher đã tồn tại"));
            }
            voucher.setCode(code);
        }
        if (request.usageLimit() != null && request.usageLimit() < voucher.getUsedCount()) {
            throw BusinessException.invalidField("usageLimit",
                    "Tổng lượt không được nhỏ hơn số lượt đã dùng (%d)".formatted(voucher.getUsedCount()));
        }
        apply(voucher, request);
        log.info("Admin id={} updated voucher {} (id={})", adminId, voucher.getCode(), id);
        return toResponse(voucherRepository.saveAndFlush(voucher), LocalDateTime.now(clock));
    }

    @Transactional
    public VoucherResponse setActive(Long adminId, Long id, boolean active) {
        admin(adminId);
        Voucher voucher = find(id);
        voucher.setActive(active);
        log.info("Admin id={} switched voucher {} {}", adminId, voucher.getCode(), active ? "on" : "off");
        return toResponse(voucherRepository.saveAndFlush(voucher), LocalDateTime.now(clock));
    }

    /** Only a voucher no order ever used; otherwise switch it off. */
    @Transactional
    public void delete(Long adminId, Long id) {
        admin(adminId);
        Voucher voucher = find(id);
        if (usageRepository.existsByVoucherId(id)) {
            throw new BusinessException(ErrorCode.VOUCHER_IN_USE,
                    "Voucher đã có đơn hàng sử dụng, chỉ có thể tạm ngưng");
        }
        voucherRepository.delete(voucher);
        log.info("Admin id={} deleted voucher {} (id={})", adminId, voucher.getCode(), id);
    }

    public PageResponse<VoucherUsageResponse> usages(Long adminId, Long id, Pageable pageable) {
        admin(adminId);
        find(id);
        return PageResponse.from(usageRepository.findAllByVoucherId(id, pageable).map(usage -> new VoucherUsageResponse(
                usage.getOrder().getId(), OrderMapper.code(usage.getOrder().getId()),
                usage.getOrder().getStatus() == null ? null : usage.getOrder().getStatus().name(),
                usage.getUser().getFullname(), usage.getUser().getEmail(), usage.getDiscountAmount(),
                usage.getOrder().getTotalAmount(), usage.getUsedAt(), usage.getReleasedAt())));
    }

    private void apply(Voucher voucher, VoucherRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw BusinessException.invalidField("endsAt", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        boolean all = request.allCategories() == null || request.allCategories();
        LinkedHashSet<Category> categories = new LinkedHashSet<>();
        if (!all) {
            List<Integer> ids = request.categoryIds() == null ? List.of() : request.categoryIds().stream().distinct().toList();
            if (ids.isEmpty()) {
                throw BusinessException.invalidField("categoryIds", "Chọn ít nhất 1 danh mục áp dụng");
            }
            List<Category> found = categoryRepository.findAllById(ids);
            if (found.size() != ids.size()) {
                throw BusinessException.invalidField("categoryIds", "Có danh mục không tồn tại");
            }
            categories.addAll(found);
        }
        voucher.setName(request.name().trim());
        voucher.setDescription(request.description() == null || request.description().isBlank() ? null
                : request.description().trim());
        voucher.setDiscountPercent(request.discountPercent());
        voucher.setMaxDiscount(request.maxDiscount());
        voucher.setMinOrderAmount(request.minOrderAmount() == null ? BigDecimal.ZERO : request.minOrderAmount());
        voucher.setStartsAt(request.startsAt());
        voucher.setEndsAt(request.endsAt());
        voucher.setUsageLimit(request.usageLimit());
        voucher.setPerCustomerLimit(request.perCustomerLimit() == null ? 1 : request.perCustomerLimit());
        voucher.setAllCategories(all);
        voucher.getCategories().clear();
        voucher.getCategories().addAll(categories);
        voucher.setActive(request.active() == null || request.active());
    }

    private static String checkedCode(String raw) {
        String code = VoucherService.normalize(raw);
        if (!CODE.matcher(code).matches()) {
            throw BusinessException.invalidField("code", "Mã gồm 3–30 ký tự chữ, số, dấu - hoặc _ (không dấu cách)");
        }
        return code;
    }

    private Voucher find(Long id) {
        return voucherRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));
    }

    private User admin(Long adminId) {
        return currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
    }

    static String status(Voucher voucher, LocalDateTime now) {
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            return "PAUSED";
        }
        if (now.isBefore(voucher.getStartsAt())) {
            return "UPCOMING";
        }
        if (!now.isBefore(voucher.getEndsAt())) {
            return "ENDED";
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return "USED_UP";
        }
        return "RUNNING";
    }

    private VoucherResponse toResponse(Voucher voucher, LocalDateTime now) {
        List<VoucherResponse.CategoryRef> categories = voucher.getCategories().stream()
                .sorted(Comparator.comparing(Category::getName))
                .map(category -> new VoucherResponse.CategoryRef(category.getId(), category.getName()))
                .toList();
        return new VoucherResponse(voucher.getId(), voucher.getCode(), voucher.getName(), voucher.getDescription(),
                voucher.getDiscountPercent(), voucher.getMaxDiscount(), voucher.getMinOrderAmount(),
                voucher.getStartsAt(), voucher.getEndsAt(), voucher.getUsageLimit(), voucher.getPerCustomerLimit(),
                voucher.getUsedCount(), Boolean.TRUE.equals(voucher.getAllCategories()), categories,
                Boolean.TRUE.equals(voucher.getActive()), status(voucher, now),
                voucher.getId() == null || !usageRepository.existsByVoucherId(voucher.getId()), voucher.getCreatedAt(),
                voucher.getUpdatedAt());
    }

    private static Specification<Voucher> matching(VoucherSearchRequest filter, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String like = "%" + filter.keyword().trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("code")), like, '\\'),
                        cb.like(cb.lower(root.get("name")), like, '\\')));
            }
            if (filter.status() != null) {
                switch (filter.status()) {
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
