package com.example.Tech.service.impl.contact;

import com.example.Tech.dto.request.contact.ContactRequestCreateRequest;
import com.example.Tech.dto.request.contact.ContactRequestSearchRequest;
import com.example.Tech.dto.request.contact.ContactRequestUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.contact.ContactRequestCreatedResponse;
import com.example.Tech.dto.response.contact.ContactRequestResponse;
import com.example.Tech.entity.contact.ContactRequest;
import com.example.Tech.entity.contact.ContactStatus;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.contact.ContactRequestFilterSpecifications;
import com.example.Tech.repository.contact.ContactRequestRepository;
import com.example.Tech.service.contact.ContactRateLimiter;
import com.example.Tech.service.contact.ContactRequestService;
import com.example.Tech.service.store.StoreAccessGuard;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactRequestServiceImpl implements ContactRequestService {

    private static final int MESSAGE_MIN_LENGTH = 10;

    private final ContactRequestRepository contactRequestRepository;
    private final ContactRateLimiter rateLimiter;
    private final CurrentUserLoader currentUserLoader;
    private final StoreAccessGuard storeAccessGuard;

    @Override
    @Transactional
    public ContactRequestCreatedResponse create(Long userId, String clientIp, ContactRequestCreateRequest request) {
        String message = request.message().trim();
        if (message.length() < MESSAGE_MIN_LENGTH) {
            String text = "Nội dung từ 10 đến 2000 ký tự";
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, text, Map.of("message", text));
        }
        User sender = userId == null ? null : currentUserLoader.load(userId);
        rateLimiter.acquire(clientIp, request.email());

        ContactRequest contact = new ContactRequest();
        contact.setUser(sender);
        contact.setFullName(request.fullName().trim());
        contact.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        contact.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        contact.setTopic(request.topic());
        contact.setMessage(message);
        contactRequestRepository.saveAndFlush(contact);

        log.info("Contact request id={} topic={} from user id={}", contact.getId(), contact.getTopic(), userId);
        return new ContactRequestCreatedResponse(contact.getId(), contact.getCreatedAt());
    }

    @Override
    public PageResponse<ContactRequestResponse> search(Long staffId, ContactRequestSearchRequest filter,
                                                       Pageable pageable) {
        storeAccessGuard.orderScope(staffId);
        return PageResponse.from(contactRequestRepository
                .findAll(ContactRequestFilterSpecifications.matching(filter), pageable)
                .map(ContactRequestServiceImpl::toResponse));
    }

    @Override
    @Transactional
    public ContactRequestResponse update(Long staffId, Long id, ContactRequestUpdateRequest request) {
        User staff = storeAccessGuard.processingScope(staffId).user();
        ContactRequest contact = contactRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTACT_REQUEST_NOT_FOUND));
        if (!contact.getStatus().canMoveTo(request.status())) {
            throw new BusinessException(ErrorCode.INVALID_CONTACT_STATUS);
        }
        String note = request.staffNote() == null || request.staffNote().isBlank() ? null : request.staffNote().trim();
        if (note != null) {
            contact.setStaffNote(note);
        }
        if (request.status() == ContactStatus.RESOLVED && contact.getStaffNote() == null) {
            String text = "Vui lòng ghi lại đã xử lý thế nào trước khi chuyển sang Đã xử lý";
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, text, Map.of("staffNote", text));
        }
        contact.setStatus(request.status());
        contact.setHandledBy(staff);
        contact.setHandledAt(LocalDateTime.now());
        contactRequestRepository.saveAndFlush(contact);

        log.info("Staff id={} set contact request id={} to {}", staffId, id, request.status());
        return toResponse(contact);
    }

    private static ContactRequestResponse toResponse(ContactRequest contact) {
        User sender = contact.getUser();
        User handler = contact.getHandledBy();
        String handlerName = handler == null ? null
                : handler.getFullname() == null || handler.getFullname().isBlank()
                ? handler.getUsername() : handler.getFullname();
        return new ContactRequestResponse(
                contact.getId(),
                sender == null ? null : sender.getId(),
                sender == null ? null : sender.getUsername(),
                contact.getFullName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getTopic(),
                contact.getMessage(),
                contact.getStatus(),
                contact.getStaffNote(),
                handlerName,
                contact.getHandledAt(),
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }
}
