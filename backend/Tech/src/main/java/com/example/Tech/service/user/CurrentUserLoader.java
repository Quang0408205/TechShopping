package com.example.Tech.service.user;

import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loads the user of an access token and re-checks the account. The access token may outlive a deleted or
 * deactivated account (up to 30 min), so services for the logged-in user call this on every request.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserLoader {

    private final UserRepository userRepository;

    /** Missing user → 401 INVALID_TOKEN; deleted or deactivated → 403 ACCOUNT_DISABLED. */
    public User load(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }
}
