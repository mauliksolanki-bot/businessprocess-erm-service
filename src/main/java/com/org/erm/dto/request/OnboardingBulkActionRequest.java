package com.org.erm.dto.request;

import com.org.erm.model.OnboardingActionDecision;
import com.org.erm.model.OnboardingAdditionalApproverDesignation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OnboardingBulkActionRequest(
        @NotEmpty(message = "At least one onboarding request is required")
        @Size(max = 500, message = "At most 500 onboarding requests can be actioned at once")
        List<@NotNull(message = "Request ID is required") Long> requestIds,

        @NotNull(message = "Decision is required")
        OnboardingActionDecision decision,

        @NotBlank(message = "Comment is required")
        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment,

        OnboardingAdditionalApproverDesignation additionalApproverDesignation
) {
}
