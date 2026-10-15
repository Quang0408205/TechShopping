package com.example.Tech.repository.employee;

import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the EmployeeAssignment mapping, the active-assignment lookups and the partial unique index
 * (uq_employee_assignments_active) against the real PostgreSQL test database. Every test is rolled
 * back; the test expecting the database to refuse a row does it last (PostgreSQL aborts the
 * transaction after the error).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class EmployeeAssignmentRepositoryTest {

    @Autowired
    private EmployeeAssignmentRepository assignmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Employee employee;
    private Store storeA;
    private Store storeB;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("assignment-repo-test@techshopping.vn");
        user.setUsername("assignment-repo-test");
        user.setPasswordHash("{test}hash");
        user.setFullname("Assignment Repo Test");
        user = userRepository.save(user);

        Employee newEmployee = new Employee();
        newEmployee.setUser(user);
        employee = employeeRepository.save(newEmployee);

        storeA = storeRepository.save(store("AssignmentRepoTest A"));
        storeB = storeRepository.save(store("AssignmentRepoTest B"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findActiveStoreIdByUserId_returnsTheStoreOfTheOnlyActiveAssignment() {
        assertThat(assignmentRepository.findActiveStoreIdByUserId(employee.getUser().getId())).isEmpty();

        assign(storeA, true, null);
        entityManager.flush();
        entityManager.clear();

        Optional<Integer> storeId = assignmentRepository.findActiveStoreIdByUserId(employee.getUser().getId());
        assertThat(storeId).contains(storeA.getId());
    }

    @Test
    void findActiveStoreIdByUserId_ignoresClosedAssignments() {
        assign(storeA, true, LocalDate.of(2026, 1, 1));
        entityManager.flush();
        entityManager.clear();

        assertThat(assignmentRepository.findActiveStoreIdByUserId(employee.getUser().getId())).isEmpty();
    }

    @Test
    void findByEmployeeIdAndActiveTrueAndEndDateIsNull_findsTheOpenAssignment() {
        assign(storeA, true, LocalDate.of(2026, 1, 1));
        EmployeeAssignment open = assign(storeB, true, null);
        entityManager.flush();
        entityManager.clear();

        EmployeeAssignment found = assignmentRepository
                .findByEmployeeIdAndActiveTrueAndEndDateIsNull(employee.getId())
                .orElseThrow();
        assertThat(found.getId()).isEqualTo(open.getId());
        assertThat(found.getStore().getId()).isEqualTo(storeB.getId());
    }

    @Test
    void existsByStoreId_reflectsWhetherAnyAssignmentReferencesTheStore() {
        assertThat(assignmentRepository.existsByStoreId(storeA.getId())).isFalse();

        assign(storeA, true, null);
        entityManager.flush();

        assertThat(assignmentRepository.existsByStoreId(storeA.getId())).isTrue();
        assertThat(assignmentRepository.existsByStoreId(storeB.getId())).isFalse();
    }

    @Test
    void uniqueIndex_rejectsASecondOpenActiveAssignmentForTheSameEmployee() {
        assign(storeA, true, null);
        entityManager.flush();

        EmployeeAssignment second = new EmployeeAssignment();
        second.setEmployee(employee);
        second.setStore(storeB);
        second.setStartDate(LocalDate.of(2025, 1, 1));
        second.setActive(true);

        assertThatThrownBy(() -> {
            assignmentRepository.save(second);
            entityManager.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    private EmployeeAssignment assign(Store store, boolean active, LocalDate endDate) {
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setEmployee(employee);
        assignment.setStore(store);
        assignment.setStartDate(LocalDate.of(2025, 1, 1));
        assignment.setEndDate(endDate);
        assignment.setActive(active);
        return assignmentRepository.save(assignment);
    }

    private static Store store(String name) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("123 Đường Test");
        return store;
    }
}
