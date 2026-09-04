package com.org.erm.dto.response;

import java.util.List;

public record SelfDashboardResponse(
        Long userId,
        String username,
        String fullName,
        String designation,
        String reportingManagerFullName,
        String reportingManagerRoleName,
        List<SelfProjectAssignmentResponse> currentProjects
) {
}
