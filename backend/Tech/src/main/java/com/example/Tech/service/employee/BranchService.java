package com.example.Tech.service.employee;

import com.example.Tech.dto.request.employee.BranchEmployeeUpdateRequest;
import com.example.Tech.dto.request.employee.EmployeeSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.employee.BranchResponse;
import com.example.Tech.dto.response.employee.EmployeeResponse;
import org.springframework.data.domain.Pageable;

/**
 * What a BRANCH_MANAGER may do with their own branch. The caller's role and current assignment are read from the
 * database on every call; the store always comes from that assignment, never from the request. A manager can
 * reach only STAFF members of their own store: not themselves, not another manager, not another store.
 */
public interface BranchService {

    BranchResponse me(Long managerId);

    /** Employees whose latest assignment is at this store: working ones and those who left it. */
    PageResponse<EmployeeResponse> employees(Long managerId, EmployeeSearchRequest filter, Pageable pageable);

    EmployeeResponse updateEmployee(Long managerId, Long employeeId, BranchEmployeeUpdateRequest request);

    /** Ends the assignment, marks the employee as left and locks the account; sessions are revoked. */
    EmployeeResponse deactivateEmployee(Long managerId, Long employeeId);
}
