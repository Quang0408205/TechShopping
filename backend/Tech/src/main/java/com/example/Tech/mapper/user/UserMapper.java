package com.example.Tech.mapper.user;

import com.example.Tech.dto.request.auth.RegisterRequest;
import com.example.Tech.dto.request.user.UserUpdateRequest;
import com.example.Tech.dto.response.auth.AuthUserResponse;
import com.example.Tech.dto.response.user.UserResponse;
import com.example.Tech.entity.user.User;
import com.example.Tech.util.AccountUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Maps simple fields only; the password hash and roles are handled by the service.
 */
@Component
public class UserMapper {

    public User toEntity(RegisterRequest request) {
        User user = new User();
        user.setEmail(AccountUtil.normalize(request.email()));
        user.setUsername(AccountUtil.normalize(request.username()));
        user.setFullname(request.fullname().trim());
        user.setPhone(trimToNull(request.phone()));
        return user;
    }

    /** Email and username are never changed here. */
    public void updateEntity(User user, UserUpdateRequest request) {
        user.setFullname(request.fullname().trim());
        user.setPhone(trimToNull(request.phone()));
        user.setAvatarUrl(trimToNull(request.avatarUrl()));
    }

    public AuthUserResponse toAuthUserResponse(User user, List<String> roles) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullname(),
                roles
        );
    }

    public UserResponse toResponse(User user, List<String> roles) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullname(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getActive(),
                roles,
                user.getLastLogin(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
