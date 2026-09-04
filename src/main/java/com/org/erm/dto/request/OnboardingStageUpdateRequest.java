package com.org.erm.dto.request;

import com.org.erm.model.OnboardingInterviewStage;
import jakarta.validation.constraints.NotNull;

public record OnboardingStageUpdateRequest(
        @NotNull(message = "Interview stage is required")
        OnboardingInterviewStage interviewStage
) {
}