package com.example.Tech.service.stock;

import com.example.Tech.dto.response.stock.StockAlertRunResponse;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.stock.StockSubscriptionRepository;
import com.example.Tech.repository.stock.StockSubscriptionRepository.Subscription;
import com.example.Tech.repository.stock.StockSubscriptionRepository.VariantInfo;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.service.mail.ReservedAddresses;
import com.example.Tech.service.mail.StockMailService;
import com.example.Tech.service.mail.StockMailTemplates.Item;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Báo tôi khi có hàng": a signed-in customer subscribes to a variant that is out of stock; when any open store holds it
 * again, one mail goes out and the subscription is deleted (kept for a later run when the mail could not be sent).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockSubscriptionService {

    static final int MAX_PER_USER = 50;

    private final StockSubscriptionRepository repository;
    private final UserRepository userRepository;
    private final CurrentUserLoader currentUserLoader;
    private final StockMailService mailService;

    public List<Long> mine(Long userId) {
        currentUserLoader.load(userId);
        return repository.variantIdsOf(userId);
    }

    @Transactional
    public void subscribe(Long userId, Long variantId) {
        currentUserLoader.load(userId);
        VariantInfo variant = repository.findVariant(variantId);
        if (variant == null || !variant.onSale()) {
            throw new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
        if (variant.stock() > 0) {
            throw new BusinessException(ErrorCode.VARIANT_IN_STOCK);
        }
        if (repository.countOf(userId) >= MAX_PER_USER && !repository.variantIdsOf(userId).contains(variantId)) {
            throw new BusinessException(ErrorCode.STOCK_SUBSCRIPTION_LIMIT);
        }
        repository.insert(userId, variantId);
    }

    @Transactional
    public void unsubscribe(Long userId, Long variantId) {
        currentUserLoader.load(userId);
        repository.delete(userId, variantId);
    }

    /** One mail per customer listing everything of theirs that is available again. Run by the job and by an ADMIN. */
    @Transactional
    public StockAlertRunResponse run() {
        if (!repository.tryLockRun()) {
            return new StockAlertRunResponse(0, 0, 0);
        }
        List<Subscription> all = repository.findAll();
        List<Long> finished = new ArrayList<>();
        Map<Long, List<Subscription>> ready = new LinkedHashMap<>();
        int backInStock = 0;
        for (Subscription subscription : all) {
            if (!subscription.variant().onSale()) {
                finished.add(subscription.id());
            } else if (subscription.variant().stock() > 0) {
                ready.computeIfAbsent(subscription.userId(), id -> new ArrayList<>()).add(subscription);
                backInStock++;
            }
        }
        int mailed = 0;
        for (Map.Entry<Long, List<Subscription>> entry : ready.entrySet()) {
            List<Long> ids = entry.getValue().stream().map(Subscription::id).toList();
            User user = userRepository.findById(entry.getKey()).filter(User::isEnabled).orElse(null);
            if (user == null) {
                finished.addAll(ids);
                continue;
            }
            List<Item> items = entry.getValue().stream()
                    .map(s -> new Item(s.variant().productId(), s.variant().productName(), s.variant().variantName())).toList();
            if (mailService.sendBackInStock(user, items)) {
                mailed++;
                finished.addAll(ids);
            } else if (ReservedAddresses.isReserved(user.getEmail())) {
                finished.addAll(ids);
            }
        }
        repository.deleteAll(finished);
        log.info("Back-in-stock check: {} subscription(s), {} available again, {} customer(s) mailed", all.size(), backInStock, mailed);
        return new StockAlertRunResponse(all.size(), backInStock, mailed);
    }
}
