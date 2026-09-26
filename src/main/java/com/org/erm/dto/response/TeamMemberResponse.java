package com.org.erm.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TeamMemberResponse(
        Long id,
        String fullName,
        String username,
        String employeeId,
        String email,
        String department,
        String designation,
        String employmentStatus,
        LocalDate joinedDate,
        LocalDateTime updatedAt,
        String reportingManagerFullName,
        String reportingManagerRoleName,
        List<String> roles,
        String personalEmailAddress,
        String phoneNumber,
        String educationQualification,
        List<SelfProjectAssignmentResponse> currentProjects
) {
}
