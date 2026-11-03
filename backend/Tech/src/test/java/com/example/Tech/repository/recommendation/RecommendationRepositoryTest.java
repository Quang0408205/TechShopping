package com.example.Tech.repository.recommendation;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.recommendation.InteractionType;
import com.example.Tech.entity.recommendation.Recommendation;
import com.example.Tech.entity.recommendation.RecommendationType;
import com.example.Tech.entity.recommendation.UserInteraction;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Click marking and the online statistics (native SQL) against the real PostgreSQL test database. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RecommendationRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);

    @Autowired private RecommendationRepository recommendationRepository;
    @Autowired private UserInteractionRepository interactionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private EntityManager entityManager;

    private Long userId;
    private Long phoneId;
    private Long caseId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("rec-click@techshopping.vn");
        user.setUsername("rec-click");
        user.setPasswordHash("{test}hash");
        user.setFullname("Rec Click Test");
        userId = userRepository.save(user).getId();
        Category category = new Category();
        category.setName("Rec Test Danh mục");
        category.setSlug("test-rec-click");
        categoryRepository.save(category);
        phoneId = product("test-rec-phone", category);
        caseId = product("test-rec-case", category);
        entityManager.flush();
    }

    @Test
    void markClicked_marksTheLatestUnclickedImpressionOfThatList_once() {
        Long older = show(phoneId, RecommendationType.SIMILAR, NOW.minusHours(5));
        Long latest = show(phoneId, RecommendationType.SIMILAR, NOW.minusHours(1));
        Long otherList = show(phoneId, RecommendationType.PERSONAL, NOW.minusMinutes(30));

        int first = recommendationRepository.markClicked(userId, phoneId, "SIMILAR", NOW.minusDays(1), NOW);
        int second = recommendationRepository.markClicked(userId, phoneId, "SIMILAR", NOW.minusDays(1), NOW.plusMinutes(1));
        int tooOld = recommendationRepository.markClicked(userId, phoneId, "SIMILAR", NOW.minusDays(1), NOW.plusMinutes(2));
        int notShown = recommendationRepository.markClicked(userId, caseId, "SIMILAR", NOW.minusDays(1), NOW);

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(1);
        assertThat(tooOld).isZero();
        assertThat(notShown).isZero();
        entityManager.clear();
        assertThat(recommendationRepository.findById(latest).orElseThrow().getClickedAt()).isEqualTo(NOW);
        assertThat(recommendationRepository.findById(older).orElseThrow().getClickedAt()).isEqualTo(NOW.plusMinutes(1));
        assertThat(recommendationRepository.findById(otherList).orElseThrow().getClickedAt()).isNull();
    }

    @Test
    void onlineStats_countsImpressionsClicksUsersAndConversionsWithinSevenDays() {
        show(phoneId, RecommendationType.PERSONAL, NOW.minusDays(2));
        show(caseId, RecommendationType.PERSONAL, NOW.minusDays(2));
        show(phoneId, RecommendationType.PERSONAL, NOW.minusDays(40));
        recommendationRepository.markClicked(userId, phoneId, "PERSONAL", NOW.minusDays(3), NOW.minusDays(2));
        recommendationRepository.markClicked(userId, caseId, "PERSONAL", NOW.minusDays(3), NOW.minusDays(2));
        // the phone is added to the cart a day after the click; the case only 9 days later (not counted)
        interactionRepository.save(new UserInteraction(userId, phoneId, InteractionType.ADD_TO_CART, NOW.minusDays(1)));
        interactionRepository.save(new UserInteraction(userId, caseId, InteractionType.ADD_TO_CART, NOW.plusDays(7)));
        entityManager.flush();

        List<RecommendationRepository.OnlineStatsRow> rows = recommendationRepository.onlineStats(NOW.minusDays(30));

        assertThat(rows).hasSize(1);
        RecommendationRepository.OnlineStatsRow personal = rows.getFirst();
        assertThat(personal.getType()).isEqualTo("PERSONAL");
        assertThat(personal.getImpressions()).isEqualTo(2);
        assertThat(personal.getClicks()).isEqualTo(2);
        assertThat(personal.getUsers()).isEqualTo(1);
        assertThat(personal.getConverted()).isEqualTo(1);
    }

    private Long show(Long productId, RecommendationType type, LocalDateTime at) {
        Long id = recommendationRepository.save(new Recommendation(userId, productId, type, new BigDecimal("0.5"), "test", at))
                .getId();
        entityManager.flush();
        return id;
    }

    private Long product(String slug, Category category) {
        Product product = new Product();
        product.setName(slug);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        return productRepository.save(product).getId();
    }
}
