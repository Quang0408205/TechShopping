package com.example.Tech.service.impl.review;

import com.example.Tech.dto.response.review.ReviewSummaryResponse;
import com.example.Tech.entity.product.Product;
import com.example.Tech.repository.review.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewStatsTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ProductReviewStats stats;

    @Test
    void summary_listsEveryStarLevelFrom5To1_withZeros_andTheRoundedAverage() {
        when(reviewRepository.countVisibleByRating(7L)).thenReturn(List.of(
                new Object[]{(short) 5, 1L}, new Object[]{(short) 4, 2L}));
        when(reviewRepository.countVisibleWithImages(7L)).thenReturn(1L);

        ReviewSummaryResponse summary = stats.summary(7L);

        assertThat(summary.averageRating()).isEqualByComparingTo("4.33");
        assertThat(summary.totalReviews()).isEqualTo(3);
        assertThat(summary.withImages()).isEqualTo(1);
        assertThat(summary.stars()).extracting(ReviewSummaryResponse.StarCount::stars).containsExactly(5, 4, 3, 2, 1);
        assertThat(summary.stars()).extracting(ReviewSummaryResponse.StarCount::count).containsExactly(1L, 2L, 0L, 0L, 0L);
    }

    @Test
    void refresh_setsTheProductAverageAndCount_andZeroWhenNoVisibleReview() {
        Product product = new Product();
        product.setId(7L);
        when(reviewRepository.countVisibleByRating(7L)).thenReturn(List.<Object[]>of(
                new Object[]{(short) 5, 2L}, new Object[]{(short) 2, 1L}));

        stats.refresh(product);

        assertThat(product.getRating()).isEqualByComparingTo("4.00");
        assertThat(product.getTotalReviews()).isEqualTo(3);

        when(reviewRepository.countVisibleByRating(7L)).thenReturn(List.of());
        stats.refresh(product);

        assertThat(product.getRating()).isEqualTo(new BigDecimal("0.00"));
        assertThat(product.getTotalReviews()).isZero();
    }
}
