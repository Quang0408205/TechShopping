package com.example.Tech.service.impl.auth;

import com.example.Tech.dto.request.auth.LoginRequest;
import com.example.Tech.dto.request.auth.RefreshTokenRequest;
import com.example.Tech.dto.request.auth.RegisterRequest;
import com.example.Tech.dto.response.auth.AuthResponse;
import com.example.Tech.entity.user.CustomerProfile;
import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.user.UserMapper;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.JwtTokenService;
import com.example.Tech.security.RefreshTokenService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZONE);

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, roleRepository, userRoleRepository,
                customerProfileRepository, passwordEncoder, jwtTokenService, refreshTokenService,
                new UserMapper(), CLOCK);
    }

    private User existingUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("an@example.com");
        user.setUsername("an.nguyen");
        user.setFullname("Nguyễn Văn An");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return user;
    }

    private static Role role(Integer id, String name) {
        Role role = new Role(name, null);
        role.setId(id);
        return role;
    }

    private void stubTokens() {
        when(jwtTokenService.issueAccessToken(any(User.class), any())).thenReturn("access-token");
        when(jwtTokenService.accessTokenTtlSeconds()).thenReturn(1800L);
        when(refreshTokenService.issue(anyLong())).thenReturn("refresh-token");
    }

    private void stubRoles(User user, String... roleNames) {
        when(userRoleRepository.findRoleNamesByUserId(user.getId())).thenReturn(List.of(roleNames));
    }

    private static void assertError(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(expected);
    }

    // ---------- register ----------

    @Test
    void register_createsUserRoleAndProfile_withNormalizedIdentity() {
        RegisterRequest request = new RegisterRequest(" An@Example.COM ", " An.Nguyen ", PASSWORD,
                " Nguyễn Văn An ", " ");
        when(userRepository.existsByEmail("an@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("an.nguyen")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(role(1, "CUSTOMER")));
        stubTokens();

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("an@example.com");
        assertThat(saved.getUsername()).isEqualTo("an.nguyen");
        assertThat(saved.getFullname()).isEqualTo("Nguyễn Văn An");
        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getActive()).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRole().getName()).isEqualTo("CUSTOMER");
        assertThat(roleCaptor.getValue().getUser()).isSameAs(saved);

        ArgumentCaptor<CustomerProfile> profileCaptor = ArgumentCaptor.forClass(CustomerProfile.class);
        verify(customerProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getUser()).isSameAs(saved);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800L);
        assertThat(response.user().id()).isEqualTo(10L);
        assertThat(response.user().roles()).containsExactly("CUSTOMER");
        verify(jwtTokenService).issueAccessToken(saved, List.of("CUSTOMER"));
        verify(refreshTokenService).issue(10L);
    }

    @Test
    void register_duplicateEmail_throws409() {
        when(userRepository.existsByEmail("an@example.com")).thenReturn(true);

        assertError(() -> authService.register(
                new RegisterRequest("AN@example.com", "an2", PASSWORD, "An", null)), ErrorCode.DUPLICATE_EMAIL);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_duplicateUsername_throws409() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("an.nguyen")).thenReturn(true);

        assertError(() -> authService.register(
                new RegisterRequest("new@example.com", "An.Nguyen", PASSWORD, "An", null)), ErrorCode.DUPLICATE_USERNAME);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_passwordLongerThan72Bytes_isRejected() {
        String longVietnamesePassword = "mậtkhẩu".repeat(10); // 70 characters, more than 72 bytes in UTF-8

        assertError(() -> authService.register(
                new RegisterRequest("new@example.com", "newuser", longVietnamesePassword, "An", null)),
                ErrorCode.VALIDATION_ERROR);
        verify(userRepository, never()).saveAndFlush(any());
    }

    // ---------- login ----------

    @Test
    void login_byEmail_returnsTokensAndUpdatesLastLogin() {
        User user = existingUser(5L);
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(user));
        stubRoles(user, "CUSTOMER");
        stubTokens();

        AuthResponse response = authService.login(new LoginRequest(" AN@example.com ", PASSWORD));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().username()).isEqualTo("an.nguyen");
        assertThat(response.user().roles()).containsExactly("CUSTOMER");
        assertThat(user.getLastLogin()).isEqualTo(LocalDateTime.of(2026, 9, 25, 17, 0));
    }

    @Test
    void login_byUsername_isCaseInsensitive_andReturnsAllRoles() {
        User user = existingUser(5L);
        when(userRepository.findByUsername("an.nguyen")).thenReturn(Optional.of(user));
        stubRoles(user, "ADMIN", "STAFF");
        stubTokens();

        AuthResponse response = authService.login(new LoginRequest("An.Nguyen", PASSWORD));

        assertThat(response.user().roles()).containsExactly("ADMIN", "STAFF");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByUsername("an.nguyen")).thenReturn(Optional.of(existingUser(5L)));

        assertError(() -> authService.login(new LoginRequest("an.nguyen", "wrong-password")),
                ErrorCode.INVALID_CREDENTIALS);
        verify(refreshTokenService, never()).issue(anyLong());
    }

    @Test
    void login_unknownUser_throwsInvalidCredentials() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertError(() -> authService.login(new LoginRequest("ghost@example.com", PASSWORD)),
                ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_disabledOrDeletedUser_throwsAccountDisabled() {
        User disabled = existingUser(5L);
        disabled.setActive(false);
        when(userRepository.findByUsername("an.nguyen")).thenReturn(Optional.of(disabled));
        assertError(() -> authService.login(new LoginRequest("an.nguyen", PASSWORD)), ErrorCode.ACCOUNT_DISABLED);

        User deleted = existingUser(6L);
        deleted.setDeletedAt(LocalDateTime.now());
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(deleted));
        assertError(() -> authService.login(new LoginRequest("an@example.com", PASSWORD)), ErrorCode.ACCOUNT_DISABLED);

        verify(refreshTokenService, never()).issue(anyLong());
    }

    @Test
    void login_disabledUserWithWrongPassword_stillReportsInvalidCredentials() {
        User disabled = existingUser(5L);
        disabled.setActive(false);
        when(userRepository.findByUsername("an.nguyen")).thenReturn(Optional.of(disabled));

        assertError(() -> authService.login(new LoginRequest("an.nguyen", "wrong-password")),
                ErrorCode.INVALID_CREDENTIALS);
    }

    // ---------- refresh / logout ----------

    @Test
    void refresh_consumesOldTokenAndIssuesNewPair() {
        User user = existingUser(5L);
        when(refreshTokenService.consume("old-refresh")).thenReturn(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        stubRoles(user, "CUSTOMER");
        stubTokens();

        AuthResponse response = authService.refresh(new RefreshTokenRequest("old-refresh"));

        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenService).consume("old-refresh");
        verify(refreshTokenService).issue(5L);
    }

    @Test
    void refresh_invalidToken_propagatesInvalidToken() {
        when(refreshTokenService.consume("bad")).thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        assertError(() -> authService.refresh(new RefreshTokenRequest("bad")), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void refresh_forDisabledUser_throwsAccountDisabled_withoutNewTokens() {
        User user = existingUser(5L);
        user.setActive(false);
        when(refreshTokenService.consume("old-refresh")).thenReturn(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        assertError(() -> authService.refresh(new RefreshTokenRequest("old-refresh")), ErrorCode.ACCOUNT_DISABLED);
        verify(refreshTokenService, never()).issue(anyLong());
    }

    @Test
    void refresh_forMissingUser_throwsInvalidToken() {
        when(refreshTokenService.consume("old-refresh")).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertError(() -> authService.refresh(new RefreshTokenRequest("old-refresh")), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void logout_revokesTheRefreshToken() {
        authService.logout(new RefreshTokenRequest("refresh-token"));

        verify(refreshTokenService).revoke(eq("refresh-token"));
    }
}
