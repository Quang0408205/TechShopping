package com.example.Tech.service.impl.review;

import com.example.Tech.dto.request.review.ReviewVisibilityRequest;
import com.example.Tech.dto.response.review.AdminReviewResponse;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.review.Review;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.review.ReviewImageRepository;
import com.example.Tech.repository.review.ReviewRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CurrentUserLoader currentUserLoader;

    @Mock
    private ProductReviewStats productReviewStats;

    @InjectMocks
    private AdminReviewServiceImpl service;

    private User admin;
    private Product product;
    private Review review;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setFullname("Quản Trị");
        User author = new User();
        author.setId(5L);
        author.setUsername("khach");
        author.setFullname("Nguyễn Văn An");
        product = new Product();
        product.setId(7L);
        product.setName("iPhone Test");
        review = new Review();
        review.setId(30L);
        review.setUser(author);
        review.setProduct(product);
        review.setRating((short) 1);
        review.setComment("Hàng tệ, đừng mua ở đây.");
        when(currentUserLoader.loadWithAnyRole(1L, RoleName.ADMIN)).thenReturn(admin);
        when(reviewRepository.findById(30L)).thenReturn(Optional.of(review));
        when(orderItemRepository.findBuyerProductPairs(anyCollection(), anyCollection(), eq(OrderStatus.DELIVERED)))
                .thenReturn(List.<Object[]>of(new Object[]{5L, 7L}));
    }

    @Test
    void hide_storesReasonAndWho_underTheProductLock_thenRecomputes() {
        AdminReviewResponse response = service.setVisibility(1L, 30L,
                new ReviewVisibilityRequest(true, "  Ngôn từ xúc phạm  "));

        InOrder order = inOrder(productRepository, reviewRepository, productReviewStats);
        order.verify(productRepository).findAllByIdInForUpdate(List.of(7L));
        order.verify(reviewRepository).flush();
        order.verify(productReviewStats).refresh(product);
        assertThat(review.getHidden()).isTrue();
        assertThat(review.getHiddenReason()).isEqualTo("Ngôn từ xúc phạm");
        assertThat(review.getHiddenBy()).isSameAs(admin);
        assertThat(review.getHiddenAt()).isNotNull();
        assertThat(review.getUpdatedAt()).isNull();
        assertThat(response.hiddenByName()).isEqualTo("Quản Trị");
        assertThat(response.verifiedPurchase()).isTrue();
    }

    @Test
    void show_clearsTheHideDetails_andRecomputes() {
        review.setHidden(true);
        review.setHiddenReason("Spam");
        review.setHiddenBy(admin);
        review.setHiddenAt(LocalDateTime.now());

        AdminReviewResponse response = service.setVisibility(1L, 30L, new ReviewVisibilityRequest(false, "bỏ qua"));

        assertThat(review.getHidden()).isFalse();
        assertThat(review.getHiddenReason()).isNull();
        assertThat(review.getHiddenBy()).isNull();
        assertThat(review.getHiddenAt()).isNull();
        assertThat(response.hidden()).isFalse();
        verify(productReviewStats).refresh(product);
    }

    @Test
    void hideWithoutReason_orUnknownReview_changesNothing() {
        assertThatThrownBy(() -> service.setVisibility(1L, 30L, new ReviewVisibilityRequest(true, "   ")))
                .satisfies(ex -> {
                    assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(((BusinessException) ex).getDetails()).containsKey("reason");
                });
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setVisibility(1L, 99L, new ReviewVisibilityRequest(false, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_FOUND);

        verify(productRepository, never()).findAllByIdInForUpdate(any());
        verify(productReviewStats, never()).refresh(any());
        assertThat(review.getHidden()).isFalse();
    }

    @Test
    void nonAdmin_isRefused_beforeReadingAnything() {
        when(currentUserLoader.loadWithAnyRole(8L, RoleName.ADMIN)).thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

        assertThatThrownBy(() -> service.setVisibility(8L, 30L, new ReviewVisibilityRequest(true, "Spam")))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(reviewRepository, never()).findById(anyLong());
    }
}
