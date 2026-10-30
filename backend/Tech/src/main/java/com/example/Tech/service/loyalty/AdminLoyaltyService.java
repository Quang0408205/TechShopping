package com.example.Tech.service.loyalty;

import com.example.Tech.dto.request.loyalty.LoyaltyTiersUpdateRequest;
import com.example.Tech.dto.response.loyalty.LoyaltyTierResponse;
import com.example.Tech.entity.loyalty.LoyaltyTier;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.repository.loyalty.LoyaltyTierRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ADMIN edits the tier thresholds + point multipliers (the 4 tiers themselves are fixed). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLoyaltyService {

    private final LoyaltyTierRepository tierRepository;
    private final CurrentUserLoader currentUserLoader;

    @Transactional(readOnly = true)
    public List<LoyaltyTierResponse> tiers(Long adminId) {
        currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        return tierRepository.findAllByOrderBySortOrderAsc().stream().map(LoyaltyService::toResponse).toList();
    }

    /**
     * Saves every tier: all codes once, the lowest tier at 0, each next threshold higher than the one before.
     * Customers whose spending now reaches a higher tier move up; nobody moves down.
     */
    @Transactional
    public List<LoyaltyTierResponse> update(Long adminId, LoyaltyTiersUpdateRequest request) {
        currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        List<LoyaltyTier> tiers = tierRepository.findAllByOrderBySortOrderAsc();
        Map<String, LoyaltyTiersUpdateRequest.Tier> sent = new HashMap<>();
        for (LoyaltyTiersUpdateRequest.Tier tier : request.tiers()) {
            if (sent.put(tier.code(), tier) != null) {
                throw BusinessException.invalidField("tiers", "Mã hạng %s bị gửi hai lần".formatted(tier.code()));
            }
        }
        if (sent.size() != tiers.size() || !tiers.stream().allMatch(t -> sent.containsKey(t.getCode()))) {
            throw BusinessException.invalidField("tiers", "Vui lòng gửi đủ các hạng");
        }
        LoyaltyTiersUpdateRequest.Tier previous = null;
        for (LoyaltyTier tier : tiers) {
            LoyaltyTiersUpdateRequest.Tier wanted = sent.get(tier.getCode());
            if (previous == null && wanted.minSpent().signum() != 0) {
                throw BusinessException.invalidField("tiers." + tier.getCode() + ".minSpent",
                        "Hạng %s là hạng khởi đầu, ngưỡng phải là 0".formatted(tier.getName()));
            }
            if (previous != null && wanted.minSpent().compareTo(previous.minSpent()) <= 0) {
                throw BusinessException.invalidField("tiers." + tier.getCode() + ".minSpent",
                        "Ngưỡng hạng %s phải cao hơn hạng trước".formatted(tier.getName()));
            }
            previous = wanted;
        }
        for (LoyaltyTier tier : tiers) {
            LoyaltyTiersUpdateRequest.Tier wanted = sent.get(tier.getCode());
            tier.setMinSpent(wanted.minSpent());
            tier.setPointMultiplier(wanted.pointMultiplier());
        }
        tierRepository.flush();
        int raised = tierRepository.raiseTiersToSpending();
        log.info("Admin id={} saved the loyalty tiers; {} customer(s) moved up", adminId, raised);
        return tierRepository.findAllByOrderBySortOrderAsc().stream().map(LoyaltyService::toResponse).toList();
    }
}
