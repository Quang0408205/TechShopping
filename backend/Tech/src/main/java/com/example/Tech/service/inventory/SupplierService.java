package com.example.Tech.service.inventory;

import com.example.Tech.dto.request.inventory.SupplierRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.SupplierResponse;
import com.example.Tech.entity.inventory.Supplier;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.inventory.PurchaseReceiptRepository;
import com.example.Tech.repository.inventory.SupplierRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Suppliers: everyone on the staff reads them (to fill a receipt), only an ADMIN changes them. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseReceiptRepository receiptRepository;
    private final CurrentUserLoader currentUserLoader;

    public PageResponse<SupplierResponse> list(String keyword, Boolean active, Pageable pageable) {
        Specification<Supplier> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                parts.add(cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("taxCode"), "")), like)));
            }
            if (active != null) {
                parts.add(cb.equal(root.get("active"), active));
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
        return PageResponse.from(supplierRepository.findAll(spec, pageable).map(SupplierService::toResponse));
    }

    public SupplierResponse get(Integer id) {
        return toResponse(find(id));
    }

    @Transactional
    public SupplierResponse create(Long userId, SupplierRequest request) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        String name = request.name().trim();
        ensureNameFree(name, null);
        Supplier supplier = new Supplier();
        apply(supplier, request, name);
        supplier.setActive(request.active() == null || request.active());
        Supplier saved = supplierRepository.saveAndFlush(supplier);
        log.info("User id={} created supplier id={}", userId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public SupplierResponse update(Long userId, Integer id, SupplierRequest request) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        Supplier supplier = find(id);
        String name = request.name().trim();
        ensureNameFree(name, id);
        apply(supplier, request, name);
        if (request.active() != null) {
            supplier.setActive(request.active());
        }
        return toResponse(supplierRepository.saveAndFlush(supplier));
    }

    /** A supplier with receipts is only switched off by editing it; one never used can be deleted. */
    @Transactional
    public void delete(Long userId, Integer id) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        Supplier supplier = find(id);
        if (receiptRepository.existsBySupplierId(id)) {
            throw new BusinessException(ErrorCode.SUPPLIER_HAS_RECEIPTS);
        }
        supplierRepository.delete(supplier);
        log.info("User id={} deleted supplier id={}", userId, id);
    }

    private Supplier find(Integer id) {
        return supplierRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    private void ensureNameFree(String name, Integer exceptId) {
        if (supplierRepository.existsByNameIgnoreCase(name, exceptId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SUPPLIER);
        }
    }

    private static void apply(Supplier supplier, SupplierRequest request, String name) {
        supplier.setName(name);
        supplier.setPhone(trimToNull(request.phone()));
        supplier.setEmail(trimToNull(request.email()));
        supplier.setAddress(trimToNull(request.address()));
        supplier.setTaxCode(trimToNull(request.taxCode()));
        supplier.setNote(trimToNull(request.note()));
    }

    static SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getPhone(), supplier.getEmail(),
                supplier.getAddress(), supplier.getTaxCode(), supplier.getNote(), supplier.isActive(),
                supplier.getCreatedAt());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
