package com.org.erm.dto.response;

import java.util.List;
import java.time.LocalDate;

public record UserProfileResponse(
        Long id,
        String username,
        String employeeId,
        String email,
        String fullName,
        String department,
        String designation,
        LocalDate joinedDate,
        String reportingManagerFullName,
        String reportingManagerRoleName,
        List<String> roles,
        List<SelfProjectAssignmentResponse> currentProjects,
        String personalEmailAddress,
        String phoneNumber,
        String educationQualification,
        boolean bankDetailsEditWindowOpen,
        String bankDetailsEditWindowMessage
) {
}
