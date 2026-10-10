package com.example.Tech.service.employee;

import com.example.Tech.dto.request.employee.HireRequest;
import com.example.Tech.dto.response.employee.HireResponse;

/**
 * Hiring creates a NEW internal account (user + role + employee profile + store assignment) in one transaction;
 * existing customer accounts are never converted. The caller's role is re-checked in the database.
 */
public interface EmployeeHiringService {

    /** ADMIN: STAFF or BRANCH_MANAGER into any open store; 409 when the store already has a manager. */
    HireResponse hireAsAdmin(Long adminId, HireRequest request);

    /** BRANCH_MANAGER: STAFF only, always into the manager's own store (any storeId in the request is ignored). */
    HireResponse hireAsManager(Long managerId, HireRequest request);
}
