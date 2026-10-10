package com.example.Tech.service.impl.review;

import com.example.Tech.dto.request.review.ReviewRequest;
import com.example.Tech.dto.response.review.ReviewResponse;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.review.Review;
import com.example.Tech.entity.review.ReviewImage;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.review.ReviewImageRepository;
import com.example.Tech.repository.review.ReviewRepository;
import com.example.Tech.service.upload.ImageStorageService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceImplTest {

    private static final String PHOTO = "http://localhost:8080/uploads/reviews/11111111-1111-1111-1111-111111111111.jpg";
    private static final String PHOTO_2 = "http://localhost:8080/uploads/reviews/22222222-2222-2222-2222-222222222222.png";
    private static final String COMMENT = "Máy chạy mượt, pin trâu.";

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
    private ImageStorageService imageStorageService;

    @Mock
    private ProductReviewStats productReviewStats;

    @InjectMocks
    private ReviewServiceImpl service;

    private User author;
    private Product product;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(5L);
        author.setUsername("khach");
        author.setFullname("Nguyễn Văn An");
        product = new Product();
        product.setId(7L);
        product.setName("iPhone Test");
        product.setActive(true);
        when(currentUserLoader.load(5L)).thenReturn(author);
        when(productRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(product));
        when(imageStorageService.isStoredReviewImage(PHOTO)).thenReturn(true);
        when(imageStorageService.isStoredReviewImage(PHOTO_2)).thenReturn(true);
        when(orderItemRepository.findBuyerIds(eq(7L), anyCollection(), eq(OrderStatus.DELIVERED))).thenReturn(List.of());
    }

    @Test
    void create_locksTheProductBeforeTheDuplicateCheck_savesPhotosInOrder_thenRecomputesTheRating() {
        ReviewResponse response = service.create(5L, new ReviewRequest(7L, 4, "  " + COMMENT + "  ", List.of(PHOTO, PHOTO_2)));

        InOrder order = inOrder(productRepository, reviewRepository, reviewImageRepository, productReviewStats);
        order.verify(productRepository).findAllByIdInForUpdate(List.of(7L));
        order.verify(reviewRepository).existsByUser_IdAndProduct_Id(5L, 7L);
        order.verify(reviewRepository).save(any(Review.class));
        order.verify(reviewImageRepository).saveAll(any());
        order.verify(reviewRepository).flush();
        order.verify(productReviewStats).refresh(product);
        assertThat(response.comment()).isEqualTo(COMMENT);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.imageUrls()).containsExactly(PHOTO, PHOTO_2);
        assertThat(response.authorName()).isEqualTo("Nguyễn Văn An");
        assertThat(response.edited()).isFalse();
        assertThat(response.verifiedPurchase()).isFalse();
    }

    @Test
    void create_secondReviewOfTheSameProduct_isRefused_andNothingIsSaved() {
        when(reviewRepository.existsByUser_IdAndProduct_Id(5L, 7L)).thenReturn(true);

        assertCode(() -> service.create(5L, new ReviewRequest(7L, 5, COMMENT, null)), ErrorCode.ALREADY_REVIEWED);
        verify(reviewRepository, never()).save(any());
        verify(productReviewStats, never()).refresh(any());
    }

    @Test
    void create_refusesForeignPhotos_duplicatePhotos_shortComments_andHiddenProducts_beforeLocking() {
        when(imageStorageService.isStoredReviewImage("https://evil.example/a.jpg")).thenReturn(false);

        BusinessException foreign = assertCode(() -> service.create(5L,
                new ReviewRequest(7L, 5, COMMENT, List.of(PHOTO, "https://evil.example/a.jpg"))), ErrorCode.VALIDATION_ERROR);
        assertThat(foreign.getDetails()).containsKey("imageUrls");
        assertCode(() -> service.create(5L, new ReviewRequest(7L, 5, COMMENT, List.of(PHOTO, PHOTO))),
                ErrorCode.VALIDATION_ERROR);
        BusinessException shortComment = assertCode(() -> service.create(5L,
                new ReviewRequest(7L, 5, "   tốt   ", null)), ErrorCode.VALIDATION_ERROR);
        assertThat(shortComment.getDetails()).containsKey("comment");
        assertThat(assertCode(() -> service.create(5L, new ReviewRequest(null, 5, COMMENT, null)),
                ErrorCode.VALIDATION_ERROR).getDetails()).containsKey("productId");
        product.setActive(false);
        assertCode(() -> service.create(5L, new ReviewRequest(7L, 5, COMMENT, null)), ErrorCode.PRODUCT_NOT_AVAILABLE);

        verify(productRepository, never()).findAllByIdInForUpdate(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void update_replacesThePhotos_deletesOnlyDroppedFilesAfterCommit_keepsAHiddenReviewHidden() {
        Review review = review();
        review.setHidden(true);
        review.setHiddenReason("Ngôn từ không phù hợp");
        ReviewImage kept = new ReviewImage(review, PHOTO, 1);
        ReviewImage dropped = new ReviewImage(review, PHOTO_2, 2);
        when(reviewRepository.findByIdAndUser_Id(30L, 5L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findAllByReview_IdOrderByDisplayOrderAsc(30L)).thenReturn(List.of(kept, dropped));

        ReviewResponse response = service.update(5L, 30L, new ReviewRequest(null, 2, COMMENT + " Sửa lại.", List.of(PHOTO)));

        InOrder order = inOrder(reviewImageRepository, productReviewStats);
        order.verify(reviewImageRepository).deleteAll(List.of(kept, dropped));
        order.verify(reviewImageRepository).flush();
        order.verify(reviewImageRepository).saveAll(any());
        order.verify(productReviewStats).refresh(product);
        verify(imageStorageService).deleteAfterCommit(PHOTO_2);
        verify(imageStorageService, never()).deleteAfterCommit(PHOTO);
        assertThat(review.getRating()).isEqualTo((short) 2);
        assertThat(review.getUpdatedAt()).isNotNull();
        assertThat(response.edited()).isTrue();
        assertThat(response.hidden()).isTrue();
        assertThat(response.hiddenReason()).isEqualTo("Ngôn từ không phù hợp");
    }

    @Test
    void update_orDelete_ofAnotherAccountsReview_isNotFound() {
        when(reviewRepository.findByIdAndUser_Id(30L, 5L)).thenReturn(Optional.empty());

        assertCode(() -> service.update(5L, 30L, new ReviewRequest(null, 5, COMMENT, null)), ErrorCode.REVIEW_NOT_FOUND);
        assertCode(() -> service.delete(5L, 30L), ErrorCode.REVIEW_NOT_FOUND);
        verify(productRepository, never()).findAllByIdInForUpdate(any());
    }

    @Test
    void delete_removesTheReviewAndItsPhotos_thenRecomputes() {
        Review review = review();
        ReviewImage photo = new ReviewImage(review, PHOTO, 1);
        when(reviewRepository.findByIdAndUser_Id(30L, 5L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findAllByReview_IdOrderByDisplayOrderAsc(30L)).thenReturn(List.of(photo));

        service.delete(5L, 30L);

        InOrder order = inOrder(productRepository, reviewRepository, productReviewStats);
        order.verify(productRepository).findAllByIdInForUpdate(List.of(7L));
        order.verify(reviewRepository).delete(review);
        order.verify(reviewRepository).flush();
        order.verify(productReviewStats).refresh(product);
        verify(imageStorageService).deleteAfterCommit(PHOTO);
    }

    private Review review() {
        Review review = new Review();
        review.setId(30L);
        review.setUser(author);
        review.setProduct(product);
        review.setRating((short) 5);
        review.setComment(COMMENT);
        return review;
    }

    private static BusinessException assertCode(Runnable call, ErrorCode code) {
        try {
            call.run();
        } catch (BusinessException e) {
            assertThat(e.getErrorCode()).isEqualTo(code);
            return e;
        }
        throw new AssertionError("Expected " + code);
    }
}
