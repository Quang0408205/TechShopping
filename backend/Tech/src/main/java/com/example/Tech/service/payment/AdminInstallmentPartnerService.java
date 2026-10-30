package com.example.Tech.service.payment;

import com.example.Tech.dto.request.payment.InstallmentPartnerUpdateRequest;
import com.example.Tech.dto.response.payment.InstallmentPartnerResponse;
import com.example.Tech.entity.payment.InstallmentFeeRate;
import com.example.Tech.entity.payment.InstallmentPartner;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.repository.payment.InstallmentFeeRateRepository;
import com.example.Tech.repository.payment.InstallmentPartnerRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ADMIN edits the conditions and the fee table of the installment partners (the partners themselves are fixed). A fee
 * change only affects applications approved afterwards: each approved application keeps the fee it was given.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInstallmentPartnerService {

    private final InstallmentPartnerRepository partners;
    private final InstallmentFeeRateRepository feeRates;
    private final CurrentUserLoader currentUserLoader;

    @Transactional(readOnly = true)
    public List<InstallmentPartnerResponse> list(Long adminId) {
        currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        return toResponses();
    }

    @Transactional
    public InstallmentPartnerResponse update(Long adminId, String code, InstallmentPartnerUpdateRequest request) {
        currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        InstallmentPartner partner = partners.findById(code.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(com.example.Tech.exception.ErrorCode.INSTALLMENT_NOT_FOUND,
                        "Không tìm thấy đối tác trả góp %s".formatted(code)));
        if (request.maxOrderTotal() != null && request.maxOrderTotal().compareTo(request.minOrderTotal()) < 0) {
            throw BusinessException.invalidField("maxOrderTotal", "Giá trị đơn tối đa phải lớn hơn hoặc bằng tối thiểu");
        }
        Set<Integer> months = new HashSet<>();
        for (InstallmentPartnerUpdateRequest.Rate rate : request.rates()) {
            if (!months.add(rate.numMonths())) {
                throw BusinessException.invalidField("rates", "Kỳ hạn %d tháng bị nhập hai lần".formatted(rate.numMonths()));
            }
        }
        partner.setActive(request.active());
        partner.setMinOrderTotal(request.minOrderTotal());
        partner.setMaxOrderTotal(request.maxOrderTotal());
        feeRates.deleteAllByPartnerCode(partner.getCode());
        for (InstallmentPartnerUpdateRequest.Rate rate : request.rates()) {
            feeRates.save(new InstallmentFeeRate(partner.getCode(), rate.numMonths(), rate.feePercent()));
        }
        partners.saveAndFlush(partner);
        log.info("Admin id={} saved installment partner {}: {} term(s), active={}", adminId, partner.getCode(),
                request.rates().size(), request.active());
        return toResponses().stream().filter(response -> response.code().equals(partner.getCode())).findFirst().orElseThrow();
    }

    private List<InstallmentPartnerResponse> toResponses() {
        Map<String, List<InstallmentPartnerResponse.Rate>> ratesByPartner = feeRates.findAllSorted().stream()
                .collect(Collectors.groupingBy(rate -> rate.getId().getPartnerCode(),
                        Collectors.mapping(rate -> new InstallmentPartnerResponse.Rate(rate.getNumMonths(), rate.getFeePercent()),
                                Collectors.toList())));
        return partners.findAllByOrderByDisplayOrderAscCodeAsc().stream()
                .map(partner -> new InstallmentPartnerResponse(partner.getCode(), partner.getName(), partner.getType(),
                        partner.isActive(), partner.getMinOrderTotal(), partner.getMaxOrderTotal(),
                        ratesByPartner.getOrDefault(partner.getCode(), List.of())))
                .toList();
    }
}
