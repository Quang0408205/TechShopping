package com.example.Tech.repository.user;

import com.example.Tech.entity.user.CustomerProfile;
import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the User group mapping (composite key, shared primary key) against the real
 * PostgreSQL test database. Every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    private static final String TEST_ROLE = "TEST_ROLE_JPA";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        Role role = roleRepository.save(new Role(TEST_ROLE, "Role used by repository tests"));

        User newUser = new User();
        newUser.setEmail("jpa-test@techshopping.vn");
        newUser.setUsername("jpa-test");
        newUser.setPasswordHash("{test}hash");
        newUser.setFullname("JPA Test");
        user = userRepository.save(newUser);

        userRoleRepository.save(new UserRole(user, role));
        customerProfileRepository.save(new CustomerProfile(user));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void user_isStoredWithDefaults() {
        User found = userRepository.findByEmail("jpa-test@techshopping.vn").orElseThrow();

        assertThat(found.getUsername()).isEqualTo("jpa-test");
        assertThat(found.getActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getDeletedAt()).isNull();
        assertThat(userRepository.findByUsername("jpa-test")).isPresent();
        assertThat(userRepository.existsByEmail("jpa-test@techshopping.vn")).isTrue();
        assertThat(userRepository.existsByUsername("nobody-here")).isFalse();
    }

    @Test
    void userRoles_areLoadedWithTheirRole() {
        List<UserRole> userRoles = userRoleRepository.findAllByIdUserId(user.getId());

        assertThat(userRoles).hasSize(1);
        UserRole userRole = userRoles.getFirst();
        assertThat(Hibernate.isInitialized(userRole.getRole())).isTrue();
        assertThat(userRole.getRole().getName()).isEqualTo(TEST_ROLE);
        assertThat(userRole.getId().getUserId()).isEqualTo(user.getId());
        assertThat(userRole.getAssignedAt()).isNotNull();
        assertThat(userRoleRepository.existsByRoleName(TEST_ROLE)).isTrue();
    }

    @Test
    void roleNames_areReturnedSortedByName() {
        Role second = roleRepository.save(new Role("TEST_ROLE_AAA", "Sorts before TEST_ROLE_JPA"));
        userRoleRepository.save(new UserRole(userRepository.findById(user.getId()).orElseThrow(), second));
        entityManager.flush();

        assertThat(userRoleRepository.findRoleNamesByUserId(user.getId()))
                .containsExactly("TEST_ROLE_AAA", TEST_ROLE);
        assertThat(userRoleRepository.findRoleNamesByUserId(-1L)).isEmpty();
    }

    @Test
    void isEnabled_dependsOnActiveFlagAndSoftDelete() {
        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.isEnabled()).isTrue();

        found.setActive(null);
        assertThat(found.isEnabled()).isTrue();
        found.setActive(false);
        assertThat(found.isEnabled()).isFalse();
        found.setActive(true);
        found.setDeletedAt(LocalDateTime.now());
        assertThat(found.isEnabled()).isFalse();
    }

    @Test
    void customerProfile_sharesThePrimaryKeyOfTheUser() {
        CustomerProfile profile = customerProfileRepository.findById(user.getId()).orElseThrow();

        assertThat(profile.getCustomerId()).isEqualTo(user.getId());
        assertThat(profile.getLoyaltyPoints()).isZero();
        assertThat(profile.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(profile.getUser().getId()).isEqualTo(user.getId());
    }
}
