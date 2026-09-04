package com.org.erm.dto.request;

import com.org.erm.model.LeaveCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LeaveApplyRequest(
        @NotNull(message = "Leave category is required")
        LeaveCategory leaveCategory,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        @NotNull(message = "End date is required")
        LocalDate endDate,
        @NotBlank(message = "Reason is required")
        String reason
) {
}
