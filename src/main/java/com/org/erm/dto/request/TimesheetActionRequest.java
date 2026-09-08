package com.org.erm.dto.request;

import com.org.erm.model.TimesheetActionDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TimesheetActionRequest(
        @NotNull(message = "Decision is required")
        TimesheetActionDecision decision,
        @NotBlank(message = "Comment is required")
        String comment
) {
}
