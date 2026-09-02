package com.org.erm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TeamLeadProjectItemResponse(
        Long projectRequestId,
        String projectName,
        String projectCode,
        String clientName,
        String projectType,
        String priority,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        String workflowStage,
        BigDecimal activeAllocationPercent,
        long activeTeamMemberCount,
        List<TeamLeadProjectMemberResponse> teamMembers
) {
}
