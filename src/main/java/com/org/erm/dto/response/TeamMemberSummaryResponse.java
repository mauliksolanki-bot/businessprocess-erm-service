package com.org.erm.dto.response;

public record TeamMemberSummaryResponse(
        Long id,
        String fullName,
        String employeeId,
        String email,
        String department,
        String designation,
        String employmentStatus
) {
}
