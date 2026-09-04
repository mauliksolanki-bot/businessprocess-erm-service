package com.org.erm.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeResponse(
        Long id,
        String fullName,
        String username,
        String email,
        List<String> roles,
        Long primaryRoleId,
        String primaryRoleName,
        String department,
        String employmentStatus,
        Long reportingManagerUserId,
        String reportingManagerUsername,
        String reportingManagerFullName,
        String reportingManagerRoleName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
