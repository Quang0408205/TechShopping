package com.example.Tech.service.user;

import com.example.Tech.dto.request.user.UserRolesUpdateRequest;
import com.example.Tech.dto.request.user.UserSearchRequest;
import com.example.Tech.dto.request.user.UserStatusUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.user.UserResponse;
import org.springframework.data.domain.Pageable;

/**
 * User management for administrators. {@code adminId} is the id of the calling administrator (from the token).
 */
public interface AdminUserService {

    PageResponse<UserResponse> search(Long adminId, UserSearchRequest filter, Pageable pageable);

    /** Soft-deleted users are returned too. */
    UserResponse getById(Long adminId, Long userId);

    /** Deactivating also revokes every refresh token of the user. */
    UserResponse updateStatus(Long adminId, Long userId, UserStatusUpdateRequest request);

    /** Replaces the whole role set. */
    UserResponse updateRoles(Long adminId, Long userId, UserRolesUpdateRequest request);

    /** Soft delete (deleted_at + is_active = false) and revoke every refresh token. Idempotent. */
    void delete(Long adminId, Long userId);
}
