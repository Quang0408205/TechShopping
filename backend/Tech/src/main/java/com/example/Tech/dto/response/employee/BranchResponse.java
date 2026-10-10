package com.example.Tech.dto.response.employee;

/** The branch of the logged-in manager. */
public record BranchResponse(
        Integer id,
        String name,
        String address,
        String phone,
        String city,
        String district,
        boolean open,
        long activeEmployees
) {
}
