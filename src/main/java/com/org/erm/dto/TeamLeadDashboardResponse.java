package com.org.erm.dto;

import java.util.List;

public record TeamLeadDashboardResponse(
        Long teamLeadUserId,
        String teamLeadUsername,
        String teamLeadFullName,
        String teamLeadDesignation,
        long reportingEmployeesCount,
        long currentMonthLeaveCount,
        long activeTeamProjectCount,
        List<TeamLeadLeaveItemResponse> currentMonthLeaves,
        List<TeamLeadProjectItemResponse> teamProjects
) {
}
