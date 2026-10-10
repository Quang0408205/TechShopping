package com.example.Tech.mapper.employee;

import com.example.Tech.dto.response.employee.EmployeeResponse;
import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.user.User;

public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    /** {@code assignment} = the current one (null when the employee has none); its store must be loaded. */
    public static EmployeeResponse toResponse(Employee employee, EmployeeAssignment assignment) {
        User user = employee.getUser();
        EmployeeResponse.CurrentAssignment current = assignment == null ? null
                : new EmployeeResponse.CurrentAssignment(assignment.getId(), assignment.getStore().getId(),
                assignment.getStore().getName(), assignment.getPositionAtStore(), assignment.getStartDate());
        return new EmployeeResponse(
                employee.getId(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullname(),
                user.getPhone(),
                employee.getEmployeeCode(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getSalary(),
                employee.getHiringDate(),
                !Boolean.FALSE.equals(employee.getActive()),
                current,
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
