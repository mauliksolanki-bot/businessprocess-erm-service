package com.org.erm.dto;

import com.org.erm.model.OnboardingWorkflowStage;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeProfileUpdateRequestResponse(
        Long id,
        Long employeeUserId,
        String employeeUsername,
        String currentFullName,
        String currentEmail,
        String currentDepartment,
        String currentEmploymentStatus,
        String currentDesignationRoleName,
        Long currentReportingManagerUserId,
        String currentReportingManagerName,
        String requestedFullName,
        String requestedEmail,
        String requestedDepartment,
        String requestedEmploymentStatus,
        String requestedDesignationRoleName,
        Long requestedReportingManagerUserId,
        String requestedReportingManagerName,
        Long replacementTeamLeadUserId,
        String replacementTeamLeadName,
        Integer directReportsAffectedCount,
        OnboardingWorkflowStage workflowStage,
        String createdByUsername,
        List<OnboardingApprovalTrailItem> approvalTrail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
