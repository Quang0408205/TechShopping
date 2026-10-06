package com.example.Tech.service.employee;

import com.example.Tech.dto.request.employee.AssignmentRequest;
import com.example.Tech.dto.request.employee.EmployeeCreateRequest;
import com.example.Tech.dto.request.employee.EmployeeSearchRequest;
import com.example.Tech.dto.request.employee.EmployeeUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.employee.EmployeeResponse;
import org.springframework.data.domain.Pageable;

/**
 * Employee profiles (1-1 with a STAFF account) and their store assignments. Admin methods re-check that the
 * caller is still an ADMIN in the database. An employee has at most one current assignment; moving to another
 * store ends the current one (end_date = today) and starts a new one, so the history is kept.
 */
public interface EmployeeService {

    PageResponse<EmployeeResponse> search(Long adminId, EmployeeSearchRequest filter, Pageable pageable);

    EmployeeResponse getById(Long adminId, Long employeeId);

    /** The account must exist, be usable and have the STAFF role, and not have a profile yet. */
    EmployeeResponse create(Long adminId, EmployeeCreateRequest request);

    /** Full replace; active = false (đã nghỉ) also ends the current assignment. */
    EmployeeResponse update(Long adminId, Long employeeId, EmployeeUpdateRequest request);

    /** The store must be open; the same store again only changes positionAtStore. */
    EmployeeResponse assign(Long adminId, Long employeeId, AssignmentRequest request);

    /** Ends the current assignment (no-op when there is none). */
    EmployeeResponse unassign(Long adminId, Long employeeId);

    /** The caller's own profile (STAFF or ADMIN); EMPLOYEE_NOT_FOUND when they have none. */
    EmployeeResponse getMine(Long userId);
}
