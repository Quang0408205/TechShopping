package com.example.Tech.service.impl.user;

import com.example.Tech.dto.request.user.ChangePasswordRequest;
import com.example.Tech.dto.request.user.CustomerProfileUpdateRequest;
import com.example.Tech.dto.request.user.UserUpdateRequest;
import com.example.Tech.dto.response.user.CustomerProfileResponse;
import com.example.Tech.dto.response.user.UserResponse;
import com.example.Tech.entity.user.CustomerProfile;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.user.CustomerProfileMapper;
import com.example.Tech.mapper.user.UserMapper;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long USER_ID = 5L;
    private static final String PASSWORD = "Matkhau@123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userRoleRepository, customerProfileRepository,
                passwordEncoder, refreshTokenService, new UserMapper(), new CustomerProfileMapper());
        user = new User();
        user.setId(USER_ID);
        user.setEmail("an@example.com");
        user.setUsername("an.nguyen");
        user.setFullname("Nguyễn Văn An");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
    }

    private void stubUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private static void assertError(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(expected);
    }

    private static CustomerProfileUpdateRequest profileRequest() {
        return new CustomerProfileUpdateRequest(LocalDate.of(1995, 8, 20), "MALE", " 123 Nguyễn Huệ ",
                "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé", "700000", "");
    }

    // ---------- current user checks ----------

    @Test
    void getMe_returnsAccountWithRoles() {
        stubUser();
        when(userRoleRepository.findRoleNamesByUserId(USER_ID)).thenReturn(List.of("CUSTOMER"));

        UserResponse response = userService.getMe(USER_ID);

        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.username()).isEqualTo("an.nguyen");
        assertThat(response.isActive()).isTrue();
        assertThat(response.roles()).containsExactly("CUSTOMER");
    }

    @Test
    void anyCall_forMissingUser_throwsInvalidToken() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertError(() -> userService.getMe(USER_ID), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void anyCall_forDisabledOrDeletedUser_throwsAccountDisabled() {
        stubUser();
        user.setActive(false);
        assertError(() -> userService.getMe(USER_ID), ErrorCode.ACCOUNT_DISABLED);

        user.setActive(true);
        user.setDeletedAt(LocalDateTime.now());
        assertError(() -> userService.getMyProfile(USER_ID), ErrorCode.ACCOUNT_DISABLED);
    }

    // ---------- update account ----------

    @Test
    void updateMe_changesNameAndContact_butNeverEmailOrUsername() {
        stubUser();
        when(userRepository.saveAndFlush(user)).thenReturn(user);
        when(userRoleRepository.findRoleNamesByUserId(USER_ID)).thenReturn(List.of("CUSTOMER"));

        UserResponse response = userService.updateMe(USER_ID,
                new UserUpdateRequest(" Trần Thị Bình ", " ", " https://img.example/a.png "));

        assertThat(user.getFullname()).isEqualTo("Trần Thị Bình");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getAvatarUrl()).isEqualTo("https://img.example/a.png");
        assertThat(user.getEmail()).isEqualTo("an@example.com");
        assertThat(user.getUsername()).isEqualTo("an.nguyen");
        assertThat(response.fullname()).isEqualTo("Trần Thị Bình");
    }

    // ---------- change password ----------

    @Test
    void changePassword_success_hashesNewPasswordAndRevokesAllSessions() {
        stubUser();

        userService.changePassword(USER_ID, new ChangePasswordRequest(PASSWORD, "MatkhauMoi@456"));

        assertThat(passwordEncoder.matches("MatkhauMoi@456", user.getPasswordHash())).isTrue();
        verify(userRepository).saveAndFlush(user);
        verify(refreshTokenService).revokeAll(USER_ID);
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidPassword() {
        stubUser();

        assertError(() -> userService.changePassword(USER_ID, new ChangePasswordRequest("wrong", "MatkhauMoi@456")),
                ErrorCode.INVALID_PASSWORD);
        verify(refreshTokenService, never()).revokeAll(anyLong());
    }

    @Test
    void changePassword_samePassword_isRejected() {
        stubUser();

        assertError(() -> userService.changePassword(USER_ID, new ChangePasswordRequest(PASSWORD, PASSWORD)),
                ErrorCode.VALIDATION_ERROR);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void changePassword_newPasswordOver72Bytes_isRejected() {
        stubUser();

        assertError(() -> userService.changePassword(USER_ID, new ChangePasswordRequest(PASSWORD, "mậtkhẩu".repeat(10))),
                ErrorCode.VALIDATION_ERROR);
        verify(refreshTokenService, never()).revokeAll(anyLong());
    }

    // ---------- profile ----------

    @Test
    void getMyProfile_returnsProfile() {
        stubUser();
        CustomerProfile profile = new CustomerProfile(user);
        profile.setCustomerId(USER_ID);
        profile.setCity("Hà Nội");
        when(customerProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));

        CustomerProfileResponse response = userService.getMyProfile(USER_ID);

        assertThat(response.customerId()).isEqualTo(USER_ID);
        assertThat(response.city()).isEqualTo("Hà Nội");
        assertThat(response.loyaltyPoints()).isZero();
    }

    @Test
    void getMyProfile_missing_throwsProfileNotFound() {
        stubUser();
        when(customerProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertError(() -> userService.getMyProfile(USER_ID), ErrorCode.CUSTOMER_PROFILE_NOT_FOUND);
    }

    @Test
    void updateMyProfile_updatesExistingProfile_keepingReadOnlyCounters() {
        stubUser();
        CustomerProfile profile = new CustomerProfile(user);
        profile.setCustomerId(USER_ID);
        profile.setLoyaltyPoints(120);
        profile.setTotalSpent(new BigDecimal("5000000.00"));
        profile.setPostalCode("old");
        when(customerProfileRepository.findById(USER_ID)).thenReturn(Optional.of(profile));
        when(customerProfileRepository.saveAndFlush(profile)).thenReturn(profile);

        CustomerProfileResponse response = userService.updateMyProfile(USER_ID, profileRequest());

        assertThat(response.address()).isEqualTo("123 Nguyễn Huệ");
        assertThat(response.gender()).isEqualTo("MALE");
        assertThat(response.postalCode()).isEqualTo("700000");
        assertThat(response.defaultShippingAddress()).isNull();
        assertThat(response.loyaltyPoints()).isEqualTo(120);
        assertThat(response.totalSpent()).isEqualByComparingTo("5000000");
    }

    @Test
    void updateMyProfile_createsProfileWhenMissing() {
        stubUser();
        when(customerProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(customerProfileRepository.saveAndFlush(any(CustomerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateMyProfile(USER_ID, profileRequest());

        ArgumentCaptor<CustomerProfile> captor = ArgumentCaptor.forClass(CustomerProfile.class);
        verify(customerProfileRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getCity()).isEqualTo("TP. Hồ Chí Minh");
    }
}
