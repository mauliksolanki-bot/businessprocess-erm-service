package com.org.erm.dto.response;

import java.util.List;

public record TeamMemberSummaryResponse(
        Long id,
        String fullName,
        String employeeId,
        String email,
        List<String> roles,
        String department,
        String designation,
        String employmentStatus
) {
}
