package com.example.Tech.service.order;

import com.example.Tech.dto.request.order.ShippingFeeRuleRequest;
import com.example.Tech.dto.response.order.ShippingFeeRuleResponse;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.ShippingFeeRule;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.order.ShippingFeeRuleRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Home-delivery fee: the province's own fee when an ADMIN set one, else the default ({@link ShippingPolicy#FLAT_FEE}).
 * Store pickup and an order with nothing to pay for are always free.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingFeeService {

    private final ShippingFeeRuleRepository ruleRepository;
    private final CurrentUserLoader currentUserLoader;

    public BigDecimal feeFor(BigDecimal subtotal, DeliveryType deliveryType, String province) {
        BigDecimal base = ShippingPolicy.feeFor(subtotal, deliveryType);
        if (base.signum() == 0 || province == null || province.isBlank()) {
            return base;
        }
        return ruleRepository.findByProvinceNameIgnoreCase(province.trim()).map(ShippingFeeRule::getFee).orElse(base);
    }

    public List<ShippingFeeRuleResponse> list(Long userId) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        return ruleRepository.findAllByOrderByProvinceNameAsc().stream().map(ShippingFeeService::toResponse).toList();
    }

    @Transactional
    public ShippingFeeRuleResponse create(Long userId, ShippingFeeRuleRequest request) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        String name = request.provinceName().trim();
        ensureFree(name, null);
        ShippingFeeRule rule = new ShippingFeeRule();
        rule.setProvinceName(name);
        rule.setFee(request.fee());
        ShippingFeeRule saved = ruleRepository.saveAndFlush(rule);
        log.info("User id={} set the shipping fee of '{}' to {}", userId, name, request.fee());
        return toResponse(saved);
    }

    @Transactional
    public ShippingFeeRuleResponse update(Long userId, Integer id, ShippingFeeRuleRequest request) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        ShippingFeeRule rule = find(id);
        String name = request.provinceName().trim();
        ensureFree(name, id);
        rule.setProvinceName(name);
        rule.setFee(request.fee());
        return toResponse(ruleRepository.saveAndFlush(rule));
    }

    @Transactional
    public void delete(Long userId, Integer id) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        ruleRepository.delete(find(id));
    }

    private ShippingFeeRule find(Integer id) {
        return ruleRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.SHIPPING_FEE_RULE_NOT_FOUND));
    }

    private void ensureFree(String name, Integer exceptId) {
        if (ruleRepository.existsByProvinceNameIgnoreCase(name, exceptId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SHIPPING_FEE_RULE);
        }
    }

    private static ShippingFeeRuleResponse toResponse(ShippingFeeRule rule) {
        return new ShippingFeeRuleResponse(rule.getId(), rule.getProvinceName(), rule.getFee(), rule.getUpdatedAt());
    }
}
