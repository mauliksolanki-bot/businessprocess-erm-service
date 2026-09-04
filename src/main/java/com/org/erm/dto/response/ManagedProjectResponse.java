package com.org.erm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ManagedProjectResponse(
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
        String projectStatus,
        String description,
        String riskNotes,
        String createdByUsername,
        boolean hasOpenChangeRequest,
        String openChangeRequestStage,
        List<OnboardingApprovalTrailItem> approvalTrail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {
}
