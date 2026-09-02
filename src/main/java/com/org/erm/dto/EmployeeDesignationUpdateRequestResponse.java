package com.org.erm.dto;

import com.org.erm.model.OnboardingWorkflowStage;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeDesignationUpdateRequestResponse(
        Long id,
        Long employeeUserId,
        String employeeUsername,
        String employeeFullName,
        String currentDesignationRoleName,
        String requestedDesignationRoleName,
        Long requestedReportingManagerUserId,
        String requestedReportingManagerUsername,
        String requestedReportingManagerFullName,
        String requestedReportingManagerRoleName,
        OnboardingWorkflowStage workflowStage,
        String createdByUsername,
        List<OnboardingApprovalTrailItem> approvalTrail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
