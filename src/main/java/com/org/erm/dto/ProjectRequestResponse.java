package com.org.erm.dto;

import com.org.erm.model.ProjectWorkflowStage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectRequestResponse(
        Long id,
        String projectName,
        String projectCode,
        String clientName,
        String projectType,
        String priority,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        BigDecimal budgetAmount,
        String currency,
        Long deliveryManagerUserId,
        String deliveryManagerName,
        Long projectOwnerUserId,
        String projectOwnerName,
        Long projectDirectorUserId,
        String projectDirectorName,
        Long projectManagerUserId,
        String projectManagerName,
        String projectStatus,
        String description,
        String riskNotes,
        ProjectWorkflowStage workflowStage,
        String createdByUsername,
        String referBackBy,
        String referBackStage,
        String referBackComment,
        LocalDateTime referBackAt,
        List<OnboardingApprovalTrailItem> approvalTrail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {
}
