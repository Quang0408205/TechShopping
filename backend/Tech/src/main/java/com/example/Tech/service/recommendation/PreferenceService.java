package com.example.Tech.service.recommendation;

import com.example.Tech.dto.request.recommendation.PreferenceRequest;
import com.example.Tech.dto.response.product.NeedResponse;
import com.example.Tech.dto.response.recommendation.PreferenceOptionResponse;
import com.example.Tech.dto.response.recommendation.PreferenceResponse;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Need;
import com.example.Tech.entity.recommendation.UserPreference;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.NeedRepository;
import com.example.Tech.repository.recommendation.UserInteractionRepository;
import com.example.Tech.repository.recommendation.UserPreferenceRepository;
import com.example.Tech.service.product.ProductFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shopping-preference questionnaire: a customer without any activity yet answers which categories (required), price
 * levels, brands and needs they are interested in, and the AI service recommends from these answers instead of the
 * behaviour of other customers. GET /users/me/preferences tells the website whether to ask.
 */
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final UserInteractionRepository interactionRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final NeedRepository needRepository;
    private final ProductFilterService productFilterService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PreferenceResponse get(Long userId) {
        return toResponse(userId, preferenceRepository.findAllByUserIdOrderByIdAsc(userId));
    }

    /** Replaces every answer of the customer. */
    @Transactional
    public PreferenceResponse save(Long userId, PreferenceRequest request) {
        validate(request);
        LocalDateTime now = LocalDateTime.now(clock);
        List<UserPreference> rows = new ArrayList<>();
        request.categoryIds().forEach(id -> rows.add(UserPreference.category(userId, id, now)));
        request.priceLevels().forEach(level -> rows.add(UserPreference.price(userId, level, now)));
        request.brandIds().forEach(id -> rows.add(UserPreference.brand(userId, id, now)));
        request.needIds().forEach(id -> rows.add(UserPreference.need(userId, id, now)));
        preferenceRepository.deleteAllOfUser(userId);
        return toResponse(userId, preferenceRepository.saveAll(rows));
    }

    /** The saved answers to send to the AI service; empty when the customer has not answered. */
    @Transactional(readOnly = true)
    public Optional<PreferenceRequest> answers(Long userId) {
        List<UserPreference> rows = preferenceRepository.findAllByUserIdOrderByIdAsc(userId);
        PreferenceRequest answers = new PreferenceRequest(
                pick(rows, UserPreference::getCategoryId), pick(rows, UserPreference::getBrandId),
                pick(rows, UserPreference::getPriceLevel), pick(rows, UserPreference::getNeedId));
        return answers.categoryIds().isEmpty() ? Optional.empty() : Optional.of(answers);
    }

    /** Choices of the questionnaire: visible categories with products on sale, their brands and needs. */
    @Transactional(readOnly = true)
    public List<PreferenceOptionResponse> options() {
        return categoryRepository.findAll().stream()
                .filter(category -> !Boolean.FALSE.equals(category.getActive()))
                .sorted(Comparator.comparing((Category c) -> c.getDisplayOrder() == null ? Integer.MAX_VALUE
                        : c.getDisplayOrder()).thenComparing(Category::getId))
                .map(category -> new PreferenceOptionResponse(category.getId(), category.getName(),
                        category.getSlug(), brands(category.getId()), needs(category.getId())))
                .filter(option -> !option.brands().isEmpty())
                .toList();
    }

    private List<PreferenceOptionResponse.Choice> brands(Integer categoryId) {
        return brandRepository.countOnSaleByCategoryId(categoryId).stream()
                .map(row -> new PreferenceOptionResponse.Choice((Integer) row[0], (String) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
    }

    private List<PreferenceOptionResponse.Choice> needs(Integer categoryId) {
        return productFilterService.getNeeds(categoryId).stream()
                .filter(need -> need.productCount() > 0)
                .map((NeedResponse need) -> new PreferenceOptionResponse.Choice(need.id(), need.name(),
                        need.productCount()))
                .toList();
    }

    private void validate(PreferenceRequest request) {
        Map<Integer, Category> categories = categoryRepository.findAllById(request.categoryIds()).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        if (categories.size() != request.categoryIds().size()
                || categories.values().stream().anyMatch(c -> Boolean.FALSE.equals(c.getActive()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unknown or hidden category");
        }
        if (brandRepository.findAllById(request.brandIds()).size() != request.brandIds().size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unknown brand");
        }
        List<Need> needs = needRepository.findAllById(request.needIds());
        if (needs.size() != request.needIds().size()
                || needs.stream().anyMatch(need -> !categories.containsKey(need.getCategoryId()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Each need must belong to a chosen category");
        }
    }

    private PreferenceResponse toResponse(Long userId, List<UserPreference> rows) {
        List<Integer> categoryIds = pick(rows, UserPreference::getCategoryId);
        return new PreferenceResponse(!categoryIds.isEmpty(), interactionRepository.existsByUserId(userId),
                categoryIds, pick(rows, UserPreference::getBrandId), pick(rows, UserPreference::getPriceLevel),
                pick(rows, UserPreference::getNeedId));
    }

    private static <T> List<T> pick(List<UserPreference> rows, Function<UserPreference, T> answer) {
        return rows.stream().map(answer).filter(Objects::nonNull).toList();
    }
}
