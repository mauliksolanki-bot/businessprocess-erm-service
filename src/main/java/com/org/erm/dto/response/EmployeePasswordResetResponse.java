package com.org.erm.dto.response;

public record EmployeePasswordResetResponse(
        Long employeeId,
        String employeeIdCode,
        String temporaryPassword
) {
}
