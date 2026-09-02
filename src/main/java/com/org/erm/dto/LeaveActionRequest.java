package com.org.erm.dto;

import com.org.erm.model.OnboardingActionDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LeaveActionRequest(
        @NotNull(message = "Decision is required")
        OnboardingActionDecision decision,
        @NotBlank(message = "Comment is required")
        String comment
) {
}
