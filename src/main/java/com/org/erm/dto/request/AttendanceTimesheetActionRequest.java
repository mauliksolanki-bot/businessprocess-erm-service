package com.org.erm.dto.request;

import com.org.erm.model.AttendanceDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceTimesheetActionRequest(
        @NotNull(message = "Decision is required")
        AttendanceDecision decision,
        @NotBlank(message = "Comment is required")
        String comment
) {
}
