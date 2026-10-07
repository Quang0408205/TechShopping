package com.example.Tech.repository.employee;

import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the Employee mapping against the real PostgreSQL test database (techshopping_test). Every
 * test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User staff;

    @BeforeEach
    void setUp() {
        User newUser = new User();
        newUser.setEmail("employee-repo-test@techshopping.vn");
        newUser.setUsername("employee-repo-test");
        newUser.setPasswordHash("{test}hash");
        newUser.setFullname("Employee Repo Test");
        staff = userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByUserId_loadsTheUserEagerly() {
        Employee employee = new Employee();
        employee.setUser(userRepository.getReferenceById(staff.getId()));
        employee.setEmployeeCode("NV-TEST-01");
        employee.setPosition("Nhân viên bán hàng");
        employeeRepository.save(employee);
        entityManager.flush();
        entityManager.clear();

        Employee found = employeeRepository.findByUserId(staff.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getUser())).isTrue();
        assertThat(found.getUser().getId()).isEqualTo(staff.getId());
        assertThat(found.getEmployeeCode()).isEqualTo("NV-TEST-01");
    }

    @Test
    void existsByUserId_reflectsWhetherTheUserHasAnEmployeeRow() {
        assertThat(employeeRepository.existsByUserId(staff.getId())).isFalse();

        Employee employee = new Employee();
        employee.setUser(userRepository.getReferenceById(staff.getId()));
        employeeRepository.save(employee);
        entityManager.flush();

        assertThat(employeeRepository.existsByUserId(staff.getId())).isTrue();
    }
}
