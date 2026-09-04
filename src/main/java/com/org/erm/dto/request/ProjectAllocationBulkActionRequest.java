package com.org.erm.dto.request;

import com.org.erm.model.OnboardingActionDecision;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectAllocationBulkActionRequest(
        @NotEmpty(message = "At least one allocation is required")
        List<@NotNull(message = "Allocation is required") Long> allocationIds,

        @NotNull(message = "Decision is required")
        OnboardingActionDecision decision,

        @NotNull(message = "Comment is required")
        @Size(min = 1, max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
