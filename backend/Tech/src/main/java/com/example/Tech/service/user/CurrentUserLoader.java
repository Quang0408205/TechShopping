package com.example.Tech.service.user;

import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Loads the user of an access token and re-checks the account. The access token may outlive a deleted or
 * deactivated account (up to 30 min), so services for the logged-in user call this on every request.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserLoader {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    /** Missing user → 401 INVALID_TOKEN; deleted or deactivated → 403 ACCOUNT_DISABLED. */
    public User load(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }

    /**
     * Like {@link #load}, and the user must still have one of the roles in the database (the token's roles
     * may be up to 30 min old) → else 403 ACCESS_DENIED.
     */
    public User loadWithAnyRole(Long userId, RoleName... roles) {
        User user = load(userId);
        List<String> current = userRoleRepository.findRoleNamesByUserId(userId);
        if (Arrays.stream(roles).map(RoleName::name).noneMatch(current::contains)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return user;
    }
}
