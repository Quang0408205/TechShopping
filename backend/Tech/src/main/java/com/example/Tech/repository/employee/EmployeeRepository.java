package com.example.Tech.repository.employee;

import com.example.Tech.entity.employee.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /** Admin list: the account is loaded with the employees (no query per row). */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<Employee> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<Employee> findWithUserById(Long id);

    /**
     * Locks the employee row (SELECT … FOR UPDATE) while its assignment changes, so two admins moving the
     * same employee at once run one after the other instead of tripping uq_employee_assignments_active.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e join fetch e.user where e.id = :id")
    Optional<Employee> findByIdForUpdate(Long id);

    boolean existsByUserId(Long userId);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);
}
