package com.example.Tech.repository.review;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.review.Review;
import com.example.Tech.entity.review.ReviewImage;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the Review / ReviewImage mapping, and the migration's CHECK constraints, against the real
 * PostgreSQL test database (techshopping_test). This is Việc 0d step R1 (schema + entity + repository
 * only, 2026-10-05); no API/UI reads or writes these tables yet.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewImageRepository reviewImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private User author;
    private User otherAuthor;
    private User admin;
    private Product product;

    @BeforeEach
    void setUp() {
        author = userRepository.save(user("review-test-author"));
        otherAuthor = userRepository.save(user("review-test-author-2"));
        admin = userRepository.save(user("review-test-admin"));

        Category category = new Category();
        category.setName("Review Test Điện thoại");
        category.setSlug("test-review-dien-thoai");
        category = categoryRepository.save(category);

        product = new Product();
        product.setName("Review Test Sản phẩm");
        product.setSlug("test-review-san-pham");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("10000000"));
        product = productRepository.save(product);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void save_setsGeneratedIdAndTimestamp_hiddenDefaultsToFalse() {
        Review saved = reviewRepository.save(review(author, product, (short) 5, "Sản phẩm dùng rất tốt, đáng mua."));
        entityManager.flush();
        entityManager.clear();

        Review found = reviewRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getRating()).isEqualTo((short) 5);
        assertThat(found.getHidden()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNull();
    }

    @Test
    void existsAndFindByUserAndProduct_oneReviewPerAccountPerProduct() {
        reviewRepository.save(review(author, product, (short) 4, "Dùng tạm ổn, giao hàng nhanh."));
        entityManager.flush();
        entityManager.clear();

        assertThat(reviewRepository.existsByUser_IdAndProduct_Id(author.getId(), product.getId())).isTrue();
        assertThat(reviewRepository.existsByUser_IdAndProduct_Id(otherAuthor.getId(), product.getId())).isFalse();
        assertThat(reviewRepository.findByUser_IdAndProduct_Id(author.getId(), product.getId())).isPresent();
    }

    @Test
    void save_sameAccountAndProductTwice_violatesTheUniqueConstraint() {
        reviewRepository.save(review(author, product, (short) 4, "Dùng tạm ổn, giao hàng nhanh."));
        entityManager.flush();

        assertThatThrownBy(() -> {
            reviewRepository.save(review(author, product, (short) 2, "Đổi ý, thấy không ưng cho lắm."));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllByProduct_IdAndHiddenFalseOrderByCreatedAtDesc_excludesHiddenReviews() {
        Review hidden = review(author, product, (short) 1, "Rất tệ, không như mô tả sản phẩm.");
        hidden.setHidden(true);
        hidden.setHiddenReason("Ngôn từ không phù hợp");
        hidden.setHiddenBy(userRepository.getReferenceById(admin.getId()));
        reviewRepository.save(hidden);
        reviewRepository.save(review(otherAuthor, product, (short) 5, "Rất hài lòng, sẽ ủng hộ tiếp lần sau."));
        entityManager.flush();
        entityManager.clear();

        List<Review> visible = reviewRepository.findAllByProduct_IdAndHiddenFalseOrderByCreatedAtDesc(product.getId());

        assertThat(visible).extracting(Review::getUser).extracting(User::getId)
                .containsExactly(otherAuthor.getId());
    }

    @Test
    void addImage_cascadesWithTheReview_andIsReturnedInDisplayOrder() {
        Review review = review(author, product, (short) 5, "Ảnh thật giống hàng nhận được, rất đẹp.");
        review.addImage(new ReviewImage(null, "/uploads/reviews/second.jpg", 2));
        review.addImage(new ReviewImage(null, "/uploads/reviews/first.jpg", 1));
        reviewRepository.save(review);
        entityManager.flush();
        entityManager.clear();

        List<ReviewImage> images = reviewImageRepository.findAllByReview_IdOrderByDisplayOrderAsc(review.getId());

        assertThat(images).extracting(ReviewImage::getImageUrl)
                .containsExactly("/uploads/reviews/first.jpg", "/uploads/reviews/second.jpg");
        assertThat(images.getFirst().getCreatedAt()).isNotNull();
    }

    @Test
    void deletingTheReview_cascadesToItsImages() {
        Review review = review(author, product, (short) 5, "Ảnh thật giống hàng nhận được, rất đẹp.");
        review.addImage(new ReviewImage(null, "/uploads/reviews/only.jpg", 1));
        Long reviewId = reviewRepository.save(review).getId();
        entityManager.flush();
        entityManager.clear();

        reviewRepository.deleteById(reviewId);
        entityManager.flush();

        assertThat(reviewImageRepository.findAllByReview_IdOrderByDisplayOrderAsc(reviewId)).isEmpty();
    }

    @Test
    void save_ratingOutOfRange_violatesTheCheckConstraint() {
        Review review = review(author, product, (short) 6, "Rating không hợp lệ để kiểm tra check.");

        assertThatThrownBy(() -> {
            reviewRepository.save(review);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_commentTooShort_violatesTheCheckConstraint() {
        Review review = review(author, product, (short) 5, "ngắn");

        assertThatThrownBy(() -> {
            reviewRepository.save(review);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_hiddenWithoutReason_violatesTheCheckConstraint() {
        Review review = review(author, product, (short) 5, "Đánh giá ẩn nhưng thiếu lý do để kiểm tra.");
        review.setHidden(true);

        assertThatThrownBy(() -> {
            reviewRepository.save(review);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Review review(User author, Product product, short rating, String comment) {
        Review review = new Review();
        review.setUser(author);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        return review;
    }

    private static User user(String username) {
        User user = new User();
        user.setEmail(username + "@techshopping.vn");
        user.setUsername(username);
        user.setPasswordHash("{test}hash");
        user.setFullname("Review Test User");
        return user;
    }
}
