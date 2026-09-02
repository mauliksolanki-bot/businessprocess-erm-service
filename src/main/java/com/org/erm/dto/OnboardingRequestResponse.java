package com.org.erm.dto;

import com.org.erm.model.OnboardingInterviewStage;
import com.org.erm.model.OnboardingWorkflowStage;

import java.time.LocalDateTime;
import java.util.List;

public record OnboardingRequestResponse(
        Long id,
        String firstName,
        String lastName,
        String aadhaarCardNumber,
        String panCardNumber,
        String personalEmailAddress,
        String permanentAddress,
        String phoneNumber,
        String designationRoleName,
        Long reportingManagerUserId,
        String reportingManagerUsername,
        String reportingManagerFullName,
        String reportingManagerRoleName,
        String educationQualification,
        OnboardingInterviewStage interviewStage,
        OnboardingWorkflowStage workflowStage,
        String createdByUsername,
        String generatedEmployeeId,
        String generatedEmailAddress,
        String referBackBy,
        String referBackStage,
        String referBackComment,
        LocalDateTime referBackAt,
        List<OnboardingApprovalTrailItem> approvalTrail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastReminderAt,
        int reminderCount
) {
}
