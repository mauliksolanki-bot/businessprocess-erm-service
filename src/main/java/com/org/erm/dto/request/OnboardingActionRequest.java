package com.org.erm.dto.request;

import com.org.erm.model.OnboardingActionDecision;
import com.org.erm.model.OnboardingAdditionalApproverDesignation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OnboardingActionRequest(
        @NotNull(message = "Decision is required")
        OnboardingActionDecision decision,

        @NotBlank(message = "Comment is required")
        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment,

        // Only used when Admin approves at the HEAD_HR_APPROVED stage. If present, the request
        // is routed to the chosen designation for an additional approval; if absent, Admin's
        // approval finalizes the request and creates the employee profile immediately.
        OnboardingAdditionalApproverDesignation additionalApproverDesignation
) {
}
