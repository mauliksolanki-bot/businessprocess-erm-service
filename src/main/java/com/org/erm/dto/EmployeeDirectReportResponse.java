package com.org.erm.dto;

public record EmployeeDirectReportResponse(
        Long id,
        String fullName,
        String username,
        String designationRoleName,
        String employmentStatus
) {
}
