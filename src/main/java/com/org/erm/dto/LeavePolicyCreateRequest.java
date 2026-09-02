package com.org.erm.dto;

import com.org.erm.model.LeaveCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LeavePolicyCreateRequest(
        @NotNull(message = "Leave category is required")
        LeaveCategory leaveCategory,
        @NotBlank(message = "Display name is required")
        String displayName,
        @NotNull(message = "Max days per year is required")
        @Min(value = 1, message = "Max days per year must be at least 1")
        @Max(value = 365, message = "Max days per year cannot exceed 365")
        Integer maxDaysPerYear,
        @NotNull(message = "Enabled flag is required")
        Boolean enabled
) {
}
