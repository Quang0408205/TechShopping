package com.example.Tech.service.user;

import com.example.Tech.dto.request.user.ChangePasswordRequest;
import com.example.Tech.dto.request.user.CustomerProfileUpdateRequest;
import com.example.Tech.dto.request.user.UserUpdateRequest;
import com.example.Tech.dto.response.user.CustomerProfileResponse;
import com.example.Tech.dto.response.user.UserResponse;

/**
 * Self-service operations of the logged-in user (the id comes from the access token).
 */
public interface UserService {

    UserResponse getMe(Long userId);

    UserResponse updateMe(Long userId, UserUpdateRequest request);

    /** Changes the password and revokes every refresh token of the user (all sessions must log in again). */
    void changePassword(Long userId, ChangePasswordRequest request);

    CustomerProfileResponse getMyProfile(Long userId);

    /** Creates the profile when it does not exist yet. */
    CustomerProfileResponse updateMyProfile(Long userId, CustomerProfileUpdateRequest request);
}
