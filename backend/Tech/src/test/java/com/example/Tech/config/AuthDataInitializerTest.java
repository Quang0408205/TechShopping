package com.example.Tech.config;

import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.entity.user.UserRoleId;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthDataInitializerTest {

    private static final String PASSWORD = "Admin@12345";

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthDataInitializer initializer(BootstrapAdminProperties properties) {
        return new AuthDataInitializer(roleRepository, userRepository, userRoleRepository, passwordEncoder, properties);
    }

    private static BootstrapAdminProperties adminProperties() {
        return new BootstrapAdminProperties("admin@techshopping.vn", "admin", PASSWORD);
    }

    @Test
    void run_createsOnlyMissingRoles() {
        when(roleRepository.existsByName("CUSTOMER")).thenReturn(true);
        when(roleRepository.existsByName("STAFF")).thenReturn(false);
        when(roleRepository.existsByName("ADMIN")).thenReturn(false);
        when(userRoleRepository.existsByRoleName("ADMIN")).thenReturn(true);

        initializer(adminProperties()).run(null);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Role::getName).containsExactly("STAFF", "ADMIN");
        assertThat(captor.getAllValues()).allSatisfy(role -> assertThat(role.getDescription()).isNotBlank());
    }

    @Test
    void run_skipsAdmin_whenAnAdminAlreadyExists() {
        when(roleRepository.existsByName(anyString())).thenReturn(true);
        when(userRoleRepository.existsByRoleName("ADMIN")).thenReturn(true);

        initializer(adminProperties()).run(null);

        verify(roleRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void run_skipsAdmin_whenEnvironmentIsIncomplete() {
        when(roleRepository.existsByName(anyString())).thenReturn(true);
        when(userRoleRepository.existsByRoleName("ADMIN")).thenReturn(false);

        initializer(new BootstrapAdminProperties("admin@techshopping.vn", "admin", "")).run(null);
        initializer(new BootstrapAdminProperties(null, null, null)).run(null);

        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void run_skipsAdmin_whenPasswordIsTooShort() {
        when(roleRepository.existsByName(anyString())).thenReturn(true);
        when(userRoleRepository.existsByRoleName("ADMIN")).thenReturn(false);

        initializer(new BootstrapAdminProperties("admin@techshopping.vn", "admin", "short")).run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void run_skipsAdmin_whenEmailOrUsernameIsTaken() {
        when(roleRepository.existsByName(anyString())).thenReturn(true);
        when(userRoleRepository.existsByRoleName("ADMIN")).thenReturn(false);
        when(userRepository.existsByEmail("admin@techshopping.vn")).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        initializer(adminProperties()).run(null);

        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void run_createsAdmin_withHashedPasswordAndNormalizedIdentity() {
        Role adminRole = new Role("ADMIN", "System administrator");
        adminRole.setId(3);
        when(roleRepository.existsByName(anyString())).thenReturn(true);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRoleRepository.existsByRoleName("ADMIN")).thenReturn(false);
        when(userRepository.existsByEmail("admin@techshopping.vn")).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        initializer(new BootstrapAdminProperties("  Admin@TechShopping.VN ", " Admin ", PASSWORD)).run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@techshopping.vn");
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getFullname()).isEqualTo(AuthDataInitializer.ADMIN_FULLNAME);
        assertThat(admin.getActive()).isTrue();
        assertThat(admin.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, admin.getPasswordHash())).isTrue();

        ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(userRoleCaptor.capture());
        UserRole userRole = userRoleCaptor.getValue();
        assertThat(userRole.getId()).isEqualTo(new UserRoleId(1L, 3));
        assertThat(userRole.getRole()).isSameAs(adminRole);
    }
}
