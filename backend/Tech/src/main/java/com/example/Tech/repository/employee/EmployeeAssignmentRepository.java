package com.example.Tech.repository.employee;

import com.example.Tech.entity.employee.EmployeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeAssignmentRepository extends JpaRepository<EmployeeAssignment, Long> {

    Optional<EmployeeAssignment> findByEmployeeIdAndActiveTrueAndEndDateIsNull(Long employeeId);

    /** The current assignments of several employees in one query, stores loaded (admin list). */
    @Query("""
            select ea from EmployeeAssignment ea
            join fetch ea.store
            where ea.employee.id in :employeeIds and ea.active = true and ea.endDate is null
            """)
    List<EmployeeAssignment> findActiveWithStoreByEmployeeIdIn(@Param("employeeIds") Collection<Long> employeeIds);

    /**
     * The store id of the given user's current active assignment (if they are an employee with one),
     * for scoping a STAFF user's inventory/stock-in actions to their own branch.
     */
    @Query("""
            select ea.store.id from EmployeeAssignment ea
            where ea.employee.user.id = :userId and ea.active = true and ea.endDate is null
            """)
    Optional<Integer> findActiveStoreIdByUserId(@Param("userId") Long userId);

    @Query("""
            select ea from EmployeeAssignment ea
            join fetch ea.store
            where ea.employee.user.id = :userId and ea.active = true and ea.endDate is null
            """)
    Optional<EmployeeAssignment> findActiveWithStoreByUserId(@Param("userId") Long userId);

    boolean existsByStoreId(Integer storeId);

    /** Employees currently working at the store (open assignment, profile not marked as left). */
    @Query("""
            select count(ea) from EmployeeAssignment ea
            where ea.store.id = :storeId and ea.active = true and ea.endDate is null
              and (ea.employee.active is null or ea.employee.active = true)
            """)
    long countWorkingEmployees(@Param("storeId") Integer storeId);

    /** Enabled accounts holding BRANCH_MANAGER with a current assignment at the store (normally 0 or 1). */
    @Query("""
            select ea.employee.user.id from EmployeeAssignment ea
            where ea.store.id = :storeId and ea.active = true and ea.endDate is null
              and (ea.employee.active is null or ea.employee.active = true)
              and ea.employee.user.deletedAt is null
              and (ea.employee.user.active is null or ea.employee.user.active = true)
              and exists (select 1 from UserRole ur where ur.id.userId = ea.employee.user.id
                          and ur.role.name = 'BRANCH_MANAGER')
            """)
    List<Long> findActiveManagerUserIds(@Param("storeId") Integer storeId);
}
