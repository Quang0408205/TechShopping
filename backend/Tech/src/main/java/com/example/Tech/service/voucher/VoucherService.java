package com.example.Tech.service.voucher;

import com.example.Tech.dto.response.voucher.VoucherOptionResponse;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.voucher.Voucher;
import com.example.Tech.entity.voucher.VoucherUsage;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.cart.CartMapper;
import com.example.Tech.repository.cart.CartItemRepository;
import com.example.Tech.repository.cart.CartRepository;
import com.example.Tech.repository.voucher.VoucherRepository;
import com.example.Tech.repository.voucher.VoucherUsageRepository;
import com.example.Tech.service.combo.ComboCalculator;
import com.example.Tech.service.combo.ComboPricingService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Plan v5 GĐ4, customer side: the vouchers running now evaluated against the cart, a preview for a typed code,
 * and the order hooks (apply when the order is placed, give the use back when it is cancelled before delivery).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository usageRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final ComboPricingService comboPricingService;
    private final CurrentUserLoader currentUserLoader;
    private final Clock clock;

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    /** Running vouchers, applicable ones first (largest discount first), then the others ending soonest. */
    public List<VoucherOptionResponse> available(Long userId) {
        currentUserLoader.load(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Voucher> running = voucherRepository.findRunning(now);
        if (running.isEmpty()) {
            return List.of();
        }
        List<VoucherCalculator.Line> lines = cartLines(userId);
        Map<Long, Long> uses = new HashMap<>();
        for (Object[] row : usageRepository.countActiveByUser(userId, running.stream().map(Voucher::getId).toList())) {
            uses.put((Long) row[0], (Long) row[1]);
        }
        List<VoucherOptionResponse> options = new ArrayList<>();
        for (Voucher voucher : running) {
            options.add(toOption(voucher, VoucherCalculator.evaluate(voucher, lines,
                    uses.getOrDefault(voucher.getId(), 0L), now)));
        }
        options.sort(Comparator.comparing((VoucherOptionResponse option) -> !option.applicable())
                .thenComparing(VoucherOptionResponse::discount, Comparator.reverseOrder()));
        return options;
    }

    /** The voucher evaluated on the cart; 404 unknown code, 400 VOUCHER_NOT_APPLICABLE with the reason. */
    public VoucherOptionResponse preview(Long userId, String code) {
        currentUserLoader.load(userId);
        Voucher voucher = voucherRepository.findByCode(normalize(code))
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));
        VoucherCalculator.Result result = VoucherCalculator.evaluate(voucher, cartLines(userId),
                usageRepository.countByVoucherIdAndUserIdAndReleasedAtIsNull(voucher.getId(), userId),
                LocalDateTime.now(clock));
        if (!result.applies()) {
            throw notApplicable(result);
        }
        return toOption(voucher, result);
    }

    /**
     * Inside placeOrder, after the items are priced and before shipping / total: locks the voucher row, checks it on
     * the order's items, writes the per-line shares + the order fields and counts the use. Returns the discount.
     */
    @Transactional
    public BigDecimal applyToOrder(Order order, User user, String code) {
        Voucher found = voucherRepository.findByCode(normalize(code))
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));
        Voucher voucher = voucherRepository.findByIdForUpdate(found.getId()).orElseThrow();
        List<OrderItem> items = order.getItems();
        List<VoucherCalculator.Line> lines = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            lines.add(new VoucherCalculator.Line((long) i, categoryId(items.get(i).getVariant()), items.get(i).afterCombo()));
        }
        VoucherCalculator.Result result = VoucherCalculator.evaluate(voucher, lines,
                usageRepository.countByVoucherIdAndUserIdAndReleasedAtIsNull(voucher.getId(), user.getId()),
                LocalDateTime.now(clock));
        if (!result.applies()) {
            throw notApplicable(result);
        }
        result.shares().forEach((index, share) -> items.get(index.intValue()).setVoucherDiscount(share));
        order.setVoucher(voucher);
        order.setVoucherCode(voucher.getCode());
        order.setVoucherDiscount(result.discount());
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        return result.discount();
    }

    /** After the order is saved (it needs its id): the usage row. */
    @Transactional
    public void recordUse(Order order, User user) {
        if (order.getVoucher() == null) {
            return;
        }
        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(order.getVoucher());
        usage.setUser(user);
        usage.setOrder(order);
        usage.setDiscountAmount(order.getVoucherDiscount());
        usage.setUsedAt(LocalDateTime.now(clock));
        usageRepository.save(usage);
    }

    /** Order cancelled before delivery: the use counts again (no-op without a voucher or when already given back). */
    @Transactional
    public void releaseFor(Order order) {
        if (order.getVoucher() == null) {
            return;
        }
        usageRepository.findByOrderId(order.getId())
                .filter(usage -> usage.getReleasedAt() == null)
                .ifPresent(usage -> {
                    usage.setReleasedAt(LocalDateTime.now(clock));
                    voucherRepository.findByIdForUpdate(usage.getVoucher().getId())
                            .ifPresent(voucher -> voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1)));
                    log.info("Order id={} cancelled: voucher {} use given back", order.getId(), order.getVoucherCode());
                });
    }

    private List<VoucherCalculator.Line> cartLines(Long userId) {
        List<CartItem> items = cartRepository.findByUserId(userId)
                .map(cart -> cartItemRepository.findAllWithProductByCartId(cart.getId()))
                .orElse(List.of());
        ComboCalculator.Result combos = comboPricingService.priceCart(items);
        return items.stream()
                .filter(line -> cartMapper.isPurchasable(line.getVariant()))
                .map(line -> new VoucherCalculator.Line(line.getId(), categoryId(line.getVariant()),
                        cartMapper.unitPrice(line.getVariant()).multiply(BigDecimal.valueOf(line.getQuantity()))
                                .subtract(combos.discountOf(line.getId()))))
                .toList();
    }

    private static Integer categoryId(ProductVariant variant) {
        Category category = variant.getProduct().getCategory();
        return category == null ? null : category.getId();
    }

    static BusinessException notApplicable(VoucherCalculator.Result result) {
        Map<String, String> details = new HashMap<>();
        details.put("reason", result.problem().name());
        if (result.shortfall().signum() > 0) {
            details.put("shortfall", result.shortfall().toPlainString());
        }
        return new BusinessException(ErrorCode.VOUCHER_NOT_APPLICABLE, result.message(), details);
    }

    VoucherOptionResponse toOption(Voucher voucher, VoucherCalculator.Result result) {
        List<String> categories = Boolean.TRUE.equals(voucher.getAllCategories()) ? List.of()
                : voucher.getCategories().stream().map(Category::getName).sorted().toList();
        Integer remaining = voucher.getUsageLimit() == null ? null
                : Math.max(0, voucher.getUsageLimit() - voucher.getUsedCount());
        return new VoucherOptionResponse(voucher.getId(), voucher.getCode(), voucher.getName(), voucher.getDescription(),
                voucher.getDiscountPercent(), voucher.getMaxDiscount(), voucher.getMinOrderAmount(), voucher.getEndsAt(),
                Boolean.TRUE.equals(voucher.getAllCategories()), categories, remaining, result.applies(),
                result.problem() == null ? null : result.problem().name(), result.message(),
                result.eligibleSubtotal(), result.discount(), result.shortfall());
    }
}
