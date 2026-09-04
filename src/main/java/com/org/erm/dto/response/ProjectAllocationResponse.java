package com.org.erm.dto.response;

import com.org.erm.model.ProjectAllocationStatus;
import com.org.erm.model.ProjectAllocationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectAllocationResponse(
        Long id,
        String allocationCode,
        Long projectRequestId,
        String projectName,
        String projectCode,
        Long employeeUserId,
        String employeeName,
        String employeeUsername,
        String employeeEmail,
        String employeeRoleName,
        ProjectAllocationType allocationType,
        BigDecimal allocationPercent,
        LocalDate startDate,
        LocalDate endDate,
        ProjectAllocationStatus status,
        String createdByUsername,
        String dmActionBy,
        LocalDateTime dmActionAt,
        String dmComment,
        String referBackBy,
        LocalDateTime referBackAt,
        String referBackComment,
        List<OnboardingApprovalTrailItem> approvalTrail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {
}
