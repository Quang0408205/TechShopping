package com.example.Tech.service.question;

import com.example.Tech.dto.request.question.AnswerQuestionRequest;
import com.example.Tech.dto.request.question.AskQuestionRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.question.AdminQuestionResponse;
import com.example.Tech.dto.response.question.ProductQuestionResponse;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.question.ProductQuestionRepository;
import com.example.Tech.service.notification.StaffNotificationService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Questions about a product: a signed-in customer asks (at most {@link #DAILY_LIMIT} a day), staff answer, and a
 * question is public only once it has an answer and is not hidden.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQuestionService {

    static final int DAILY_LIMIT = 5;

    private final ProductQuestionRepository repository;
    private final CurrentUserLoader currentUserLoader;
    private final StaffNotificationService notificationService;
    private final Clock clock;

    public PageResponse<ProductQuestionResponse> answered(Long productId, int page, int size) {
        if (!repository.productIsOnSale(productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 20));
        List<ProductQuestionResponse> rows = repository.answeredPage(productId, pageable.getPageSize(), pageable.getOffset());
        return PageResponse.from(new PageImpl<>(rows, pageable, repository.answeredCount(productId)));
    }

    @Transactional
    public void ask(Long userId, AskQuestionRequest request) {
        currentUserLoader.load(userId);
        String question = request.question().trim().replaceAll("\\s+", " ");
        if (question.length() < 10) {
            throw BusinessException.invalidField("question", "Câu hỏi dài từ 10 đến 500 ký tự");
        }
        if (!repository.productIsOnSale(request.productId())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (repository.countSince(userId, LocalDateTime.now(clock).minusHours(24)) >= DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.QUESTION_LIMIT_REACHED);
        }
        Long id = repository.insert(request.productId(), userId, question);
        notificationService.publish(null, StaffNotificationService.Kind.PRODUCT_QUESTION,
                "Câu hỏi mới về " + repository.productName(request.productId()),
                question.length() > 120 ? question.substring(0, 120) + "…" : question, "questions.html");
        log.info("User id={} asked question id={} about product id={}", userId, id, request.productId());
    }

    public PageResponse<AdminQuestionResponse> adminList(Long userId, String status, String keyword, int page, int size) {
        requireStaff(userId);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        List<AdminQuestionResponse> rows = repository.adminPage(status, keyword, pageable.getPageSize(), pageable.getOffset());
        return PageResponse.from(new PageImpl<>(rows, pageable, repository.adminCount(status, keyword)));
    }

    @Transactional
    public AdminQuestionResponse answer(Long userId, Long id, AnswerQuestionRequest request) {
        requireStaff(userId);
        String answer = request.answer().trim();
        if (answer.isEmpty()) {
            throw BusinessException.invalidField("answer", "Vui lòng nhập câu trả lời");
        }
        if (repository.answer(id, answer, userId, LocalDateTime.now(clock)) == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_QUESTION_NOT_FOUND);
        }
        return repository.findForAdmin(id);
    }

    @Transactional
    public AdminQuestionResponse hide(Long userId, Long id, boolean hidden) {
        requireStaff(userId);
        if (repository.setHidden(id, hidden) == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_QUESTION_NOT_FOUND);
        }
        return repository.findForAdmin(id);
    }

    private void requireStaff(Long userId) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.STAFF, RoleName.BRANCH_MANAGER, RoleName.ADMIN);
    }
}
