package com.example.Tech.config;

import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.util.AccountUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Seeds the built-in roles and the first ADMIN account at startup. Idempotent: existing roles
 * and accounts are never changed. database/techshopping.sql contains no seed data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class AuthDataInitializer implements ApplicationRunner {

    static final String ADMIN_FULLNAME = "Administrator";

    private static final Map<RoleName, String> ROLE_DESCRIPTIONS = Map.of(
            RoleName.CUSTOMER, "Customer account",
            RoleName.STAFF, "Store staff",
            RoleName.ADMIN, "System administrator"
    );

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties adminProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createMissingRoles();
        createBootstrapAdmin();
    }

    private void createMissingRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName.name())) {
                roleRepository.save(new Role(roleName.name(), ROLE_DESCRIPTIONS.get(roleName)));
                log.info("Created role {}", roleName);
            }
        }
    }

    private void createBootstrapAdmin() {
        if (userRoleRepository.existsByRoleName(RoleName.ADMIN.name())) {
            log.debug("An ADMIN account already exists; bootstrap admin skipped");
            return;
        }
        if (!adminProperties.isComplete()) {
            log.warn("No ADMIN account exists and ADMIN_EMAIL, ADMIN_USERNAME, ADMIN_PASSWORD are not all set; "
                    + "bootstrap admin skipped");
            return;
        }
        String password = adminProperties.password();
        if (password.length() < AccountUtil.MIN_PASSWORD_LENGTH || AccountUtil.exceedsBcryptLimit(password)) {
            log.warn("ADMIN_PASSWORD must have at least {} characters and at most {} bytes; bootstrap admin skipped",
                    AccountUtil.MIN_PASSWORD_LENGTH, AccountUtil.MAX_PASSWORD_BYTES);
            return;
        }

        String email = AccountUtil.normalize(adminProperties.email());
        String username = AccountUtil.normalize(adminProperties.username());
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
            log.warn("A user with the bootstrap admin email or username already exists; bootstrap admin skipped");
            return;
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullname(ADMIN_FULLNAME);
        User saved = userRepository.save(admin);

        Role adminRole = roleRepository.findByName(RoleName.ADMIN.name())
                .orElseThrow(() -> new IllegalStateException("Role ADMIN is missing"));
        userRoleRepository.save(new UserRole(saved, adminRole));
        log.info("Created bootstrap ADMIN account id={} username={}", saved.getId(), username);
    }
}
