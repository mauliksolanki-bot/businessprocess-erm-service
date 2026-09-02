package com.org.erm.dto;

import com.org.erm.model.OnboardingActionDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectAllocationActionRequest(
        @NotNull(message = "Decision is required")
        OnboardingActionDecision decision,

        @NotBlank(message = "Comment is required")
        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
