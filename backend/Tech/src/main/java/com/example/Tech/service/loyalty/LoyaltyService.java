package com.example.Tech.service.loyalty;

import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.loyalty.LoyaltySummaryResponse;
import com.example.Tech.dto.response.loyalty.LoyaltyTierResponse;
import com.example.Tech.dto.response.loyalty.LoyaltyTransactionResponse;
import com.example.Tech.entity.aftersales.ReturnItem;
import com.example.Tech.entity.aftersales.ReturnRequest;
import com.example.Tech.entity.loyalty.LoyaltyTier;
import com.example.Tech.entity.loyalty.LoyaltyTransaction;
import com.example.Tech.entity.loyalty.LoyaltyTransactionType;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.user.CustomerProfile;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.aftersales.ReturnItemRepository;
import com.example.Tech.repository.aftersales.ReturnRequestRepository;
import com.example.Tech.repository.loyalty.LoyaltyTierRepository;
import com.example.Tech.repository.loyalty.LoyaltyTransactionRepository;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loyalty points + tiers. The balance lives in customer_profiles.loyalty_points (changed only with the
 * profile row locked) and every change writes a loyalty_transactions row. Tiers never go down.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyaltyService {

    private final LoyaltyTierRepository tierRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final CustomerProfileRepository profileRepository;
    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final CurrentUserLoader currentUserLoader;
    private final Clock clock;

    public LoyaltySummaryResponse summary(Long userId) {
        currentUserLoader.load(userId);
        List<LoyaltyTier> tiers = tierRepository.findAllByOrderBySortOrderAsc();
        CustomerProfile profile = profileRepository.findById(userId).orElse(null);
        int points = profile == null ? 0 : profile.getLoyaltyPoints();
        BigDecimal spent = profile == null ? BigDecimal.ZERO : profile.getTotalSpent();
        LoyaltyTier tier = profile == null ? tiers.get(0) : tierOf(tiers, profile.getTierCode());
        LoyaltyTier next = tiers.stream().filter(t -> t.getSortOrder() > tier.getSortOrder()).findFirst().orElse(null);
        BigDecimal toNext = BigDecimal.ZERO;
        int progress = 100;
        if (next != null) {
            toNext = next.getMinSpent().subtract(spent).max(BigDecimal.ZERO);
            BigDecimal span = next.getMinSpent().subtract(tier.getMinSpent());
            BigDecimal done = spent.subtract(tier.getMinSpent()).max(BigDecimal.ZERO);
            progress = span.signum() <= 0 ? 100 : done.multiply(BigDecimal.valueOf(100))
                    .divide(span, 0, RoundingMode.FLOOR).min(BigDecimal.valueOf(100)).intValue();
        }
        return new LoyaltySummaryResponse(points, LoyaltyPolicy.valueOf(points), toResponse(tier),
                next == null ? null : toResponse(next), spent, toNext, progress,
                tiers.stream().map(LoyaltyService::toResponse).toList(),
                new LoyaltySummaryResponse.Rules(LoyaltyPolicy.VND_PER_POINT_EARNED, LoyaltyPolicy.VND_PER_POINT_SPENT,
                        LoyaltyPolicy.MAX_REDEEM_PERCENT));
    }

    public PageResponse<LoyaltyTransactionResponse> history(Long userId, Pageable pageable) {
        currentUserLoader.load(userId);
        return PageResponse.from(transactionRepository.findAllByUserIdOrderByIdDesc(userId, pageable)
                .map(tx -> new LoyaltyTransactionResponse(tx.getId(), tx.getType(), tx.getPoints(), tx.getBalanceAfter(),
                        tx.getNote(), tx.getOrder() == null ? null : tx.getOrder().getId(),
                        tx.getOrder() == null ? null : OrderMapper.code(tx.getOrder().getId()), tx.getCreatedAt())));
    }

    /**
     * Inside placeOrder, after the voucher: spends {@code points} of the customer's balance (profile row locked),
     * splits their value over the lines (after the voucher shares) and writes the order fields. Returns the discount.
     */
    @Transactional
    public BigDecimal applyToOrder(Order order, User user, Integer points) {
        if (points == null || points <= 0) {
            return BigDecimal.ZERO;
        }
        Map<Integer, BigDecimal> lines = new LinkedHashMap<>();
        List<OrderItem> items = order.getItems();
        for (int i = 0; i < items.size(); i++) {
            lines.put(i, items.get(i).afterCombo().subtract(items.get(i).getVoucherDiscount()));
        }
        BigDecimal goodsAfterVoucher = lines.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        int balance = profileRepository.findByIdForUpdate(user.getId()).map(CustomerProfile::getLoyaltyPoints).orElse(0);
        if (points > balance) {
            throw BusinessException.invalidField("pointsToUse", "Bạn chỉ có %d điểm".formatted(balance));
        }
        int cap = LoyaltyPolicy.maxRedeemable(goodsAfterVoucher);
        if (points > cap) {
            throw BusinessException.invalidField("pointsToUse",
                    "Đơn này dùng được tối đa %d điểm (%d%% giá trị hàng)".formatted(cap, LoyaltyPolicy.MAX_REDEEM_PERCENT));
        }
        BigDecimal discount = LoyaltyPolicy.valueOf(points);
        LoyaltyPolicy.split(lines, discount).forEach((index, share) -> items.get(index).setPointsDiscount(share));
        order.setPointsUsed(points);
        order.setPointsDiscount(discount);
        return discount;
    }

    /** After the order is saved: takes the points off the balance with a REDEEM line. */
    @Transactional
    public void recordRedeem(Order order, User user) {
        if (order.getPointsUsed() <= 0) {
            return;
        }
        CustomerProfile profile = profileRepository.findByIdForUpdate(user.getId()).orElseThrow();
        change(profile, order, null, LoyaltyTransactionType.REDEEM, -order.getPointsUsed(),
                "Dùng điểm cho đơn " + OrderMapper.code(order.getId()));
    }

    /** Order cancelled before delivery: the points it spent come back (once). */
    @Transactional
    public void releaseFor(Order order) {
        if (order.getPointsUsed() <= 0 || transactionRepository.existsByOrderIdAndType(order.getId(), LoyaltyTransactionType.REDEEM_BACK)) {
            return;
        }
        profileRepository.findByIdForUpdate(order.getUser().getId()).ifPresent(profile -> {
            change(profile, order, null, LoyaltyTransactionType.REDEEM_BACK, order.getPointsUsed(),
                    "Hoàn điểm đơn huỷ " + OrderMapper.code(order.getId()));
            log.info("Order id={} cancelled: {} point(s) given back", order.getId(), order.getPointsUsed());
        });
    }

    /**
     * Order delivered (total_spent already includes it): earns points with the multiplier of the tier the customer
     * had before this order, then raises the tier if the new total reaches a higher one.
     */
    @Transactional
    public void onDelivered(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        Long userId = order.getUser().getId();
        CustomerProfile profile = profileRepository.findByIdForUpdate(userId).orElse(null);
        if (profile == null) {
            profileRepository.insertIfMissing(userId);
            order = orderRepository.findById(orderId).orElseThrow();
            profile = profileRepository.findByIdForUpdate(userId).orElseThrow();
        }
        if (transactionRepository.existsByOrderIdAndType(orderId, LoyaltyTransactionType.EARN)) {
            return;
        }
        List<LoyaltyTier> tiers = tierRepository.findAllByOrderBySortOrderAsc();
        LoyaltyTier tier = tierOf(tiers, profile.getTierCode());
        int earned = LoyaltyPolicy.earnedPoints(paidGoods(order), tier.getPointMultiplier());
        if (earned > 0) {
            order.setPointsEarned(earned);
            change(profile, order, null, LoyaltyTransactionType.EARN, earned,
                    "Tích điểm đơn " + OrderMapper.code(orderId) + " (hạng " + tier.getName() + " x"
                            + tier.getPointMultiplier().stripTrailingZeros().toPlainString() + ")");
        }
        LoyaltyTier reached = LoyaltyPolicy.tierFor(tiers, profile.getTotalSpent());
        if (reached.getSortOrder() > tier.getSortOrder()) {
            profile.setTierCode(reached.getCode());
            log.info("User id={} reached tier {}", userId, reached.getCode());
        }
    }

    /**
     * A return was refunded: the points earned on the refunded money are taken back (never below 0) and the points
     * spent on the returned units come back.
     */
    @Transactional
    public void onReturnRefunded(Long returnRequestId) {
        ReturnRequest request = returnRequestRepository.findById(returnRequestId).orElseThrow();
        Order order = request.getOrder();
        CustomerProfile profile = profileRepository.findByIdForUpdate(order.getUser().getId()).orElse(null);
        if (profile == null) {
            return;
        }
        BigDecimal spentPoints = BigDecimal.ZERO;
        for (ReturnItem item : returnItemRepository.findAllByReturnRequest_IdIn(List.of(returnRequestId))) {
            spentPoints = spentPoints.add(item.getOrderItem().pointsShareFor(item.getQuantity()));
        }
        int back = spentPoints.divide(LoyaltyPolicy.VND_PER_POINT_SPENT, 0, RoundingMode.FLOOR).intValueExact();
        String code = OrderMapper.code(order.getId());
        if (back > 0) {
            change(profile, order, request, LoyaltyTransactionType.REDEEM_BACK, back, "Hoàn điểm đã dùng (trả hàng đơn " + code + ")");
        }
        int revoke = Math.min(LoyaltyPolicy.revokedPoints(order.getPointsEarned(), request.getRefundAmount(),
                paidGoods(order)), profile.getLoyaltyPoints());
        if (revoke > 0) {
            change(profile, order, request, LoyaltyTransactionType.REVOKE, -revoke, "Trừ điểm đã tích (trả hàng đơn " + code + ")");
        }
    }

    /** Goods really paid: total without shipping / tax (promotions, voucher and points already taken off). */
    private static BigDecimal paidGoods(Order order) {
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal shipping = order.getShippingCost() == null ? BigDecimal.ZERO : order.getShippingCost();
        BigDecimal tax = order.getTaxAmount() == null ? BigDecimal.ZERO : order.getTaxAmount();
        return total.subtract(shipping).subtract(tax).max(BigDecimal.ZERO);
    }

    private void change(CustomerProfile profile, Order order, ReturnRequest request, LoyaltyTransactionType type,
                        int points, String note) {
        int balance = profile.getLoyaltyPoints() + points;
        profile.setLoyaltyPoints(balance);
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setUser(profile.getUser());
        tx.setOrder(order);
        tx.setReturnRequest(request);
        tx.setType(type);
        tx.setPoints(points);
        tx.setBalanceAfter(balance);
        tx.setNote(note);
        tx.setCreatedAt(LocalDateTime.now(clock));
        transactionRepository.save(tx);
    }

    static LoyaltyTier tierOf(List<LoyaltyTier> tiers, String code) {
        return tiers.stream().filter(t -> t.getCode().equals(code)).findFirst().orElse(tiers.get(0));
    }

    static LoyaltyTierResponse toResponse(LoyaltyTier tier) {
        return new LoyaltyTierResponse(tier.getCode(), tier.getName(), tier.getMinSpent(), tier.getPointMultiplier(),
                tier.getSortOrder());
    }
}
