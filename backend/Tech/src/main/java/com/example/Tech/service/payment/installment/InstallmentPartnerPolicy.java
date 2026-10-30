package com.example.Tech.service.payment.installment;

import com.example.Tech.dto.request.payment.InstallmentRequest;
import com.example.Tech.dto.response.payment.InstallmentOptionsResponse;
import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.entity.payment.InstallmentFeeRate;
import com.example.Tech.entity.payment.InstallmentPartner;
import com.example.Tech.entity.payment.InstallmentPartnerType;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.payment.InstallmentFeeRateRepository;
import com.example.Tech.repository.payment.InstallmentPartnerRepository;
import com.example.Tech.service.payment.InstallmentPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Which partners a customer may pick for an order, and whether an application fits the partner chosen. */
@Component
@RequiredArgsConstructor
public class InstallmentPartnerPolicy {

    private final InstallmentPartnerRepository partners;
    private final InstallmentFeeRateRepository feeRates;

    /** Options for the checkout form; with a total, only the partners that accept it (null = every active partner). */
    public InstallmentOptionsResponse options(BigDecimal orderTotal) {
        Map<String, List<Integer>> termsByPartner = feeRates.findAllSorted().stream()
                .collect(Collectors.groupingBy(rate -> rate.getId().getPartnerCode(),
                        Collectors.mapping(InstallmentFeeRate::getNumMonths, Collectors.toList())));
        List<InstallmentOptionsResponse.Partner> offered = partners.findAllByActiveTrueOrderByDisplayOrderAscCodeAsc().stream()
                .filter(partner -> orderTotal == null || partner.accepts(orderTotal))
                .map(partner -> new InstallmentOptionsResponse.Partner(partner.getCode(), partner.getName(),
                        partner.getType().name(), partner.getType() == InstallmentPartnerType.CREDIT_CARD,
                        termsByPartner.getOrDefault(partner.getCode(), List.of()),
                        partner.getMinOrderTotal(), partner.getMaxOrderTotal()))
                .filter(partner -> !partner.termsInMonths().isEmpty())
                .toList();
        List<Integer> allTerms = offered.stream().flatMap(partner -> partner.termsInMonths().stream())
                .distinct().sorted().toList();
        return new InstallmentOptionsResponse(
                allTerms,
                offered.stream().map(InstallmentOptionsResponse.Partner::minOrderTotal).min(BigDecimal::compareTo)
                        .orElse(InstallmentPolicy.MIN_ORDER_TOTAL),
                BigDecimal.ZERO,
                InstallmentPolicy.CITIZEN_ID_LENGTH,
                Arrays.stream(InstallmentBank.values())
                        .map(bank -> new InstallmentOptionsResponse.Bank(bank.name(), bank.getDisplayName()))
                        .toList(),
                offered);
    }

    /** The partner the application goes to; the term and the card bank must suit it. */
    public InstallmentPartner resolve(InstallmentRequest request) {
        String code = StringUtils.hasText(request.partner()) ? request.partner().trim().toUpperCase()
                : request.cardBank() != null ? "CREDIT_CARD" : null;
        if (code == null) {
            throw BusinessException.invalidField("installment.partner", "Vui lòng chọn đối tác trả góp");
        }
        InstallmentPartner partner = partners.findById(code).filter(InstallmentPartner::isActive)
                .orElseThrow(() -> BusinessException.invalidField("installment.partner", "Đối tác trả góp không tồn tại hoặc đang tạm dừng"));
        if (partner.getType() == InstallmentPartnerType.CREDIT_CARD && request.cardBank() == null) {
            throw BusinessException.invalidField("installment.cardBank", "Vui lòng chọn ngân hàng phát hành thẻ");
        }
        if (partner.getType() != InstallmentPartnerType.CREDIT_CARD && request.cardBank() != null) {
            throw BusinessException.invalidField("installment.cardBank", "%s không cần thông tin thẻ".formatted(partner.getName()));
        }
        if (feeRates.findOne(code, request.months()).isEmpty()) {
            throw BusinessException.invalidField("installment.months",
                    "%s không hỗ trợ kỳ hạn %d tháng".formatted(partner.getName(), request.months()));
        }
        return partner;
    }

    /** The order total must be inside the range the partner finances. */
    public void checkAmount(InstallmentPartner partner, BigDecimal orderTotal) {
        if (!partner.accepts(orderTotal)) {
            String range = partner.getMaxOrderTotal() == null
                    ? "từ %s".formatted(money(partner.getMinOrderTotal()))
                    : "từ %s đến %s".formatted(money(partner.getMinOrderTotal()), money(partner.getMaxOrderTotal()));
            throw new BusinessException(ErrorCode.INSTALLMENT_NOT_ELIGIBLE,
                    "%s chỉ trả góp đơn hàng %s".formatted(partner.getName(), range));
        }
    }

    private static String money(BigDecimal amount) {
        return String.format(java.util.Locale.US, "%,.0fđ", amount).replace(',', '.');
    }
}
