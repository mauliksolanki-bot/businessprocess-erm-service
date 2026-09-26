package com.org.erm.dto.response;

import java.util.List;

public record UserProfileResponse(
        Long id,
        String username,
        String employeeId,
        String email,
        String fullName,
        String designation,
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
