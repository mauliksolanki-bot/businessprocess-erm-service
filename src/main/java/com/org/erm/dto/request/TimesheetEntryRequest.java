package com.org.erm.dto.request;

import com.org.erm.model.TimesheetWorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TimesheetEntryRequest(
        @NotNull(message = "Work type is required")
        TimesheetWorkType workType,
        Long projectAllocationId,
        @NotBlank(message = "Task name is required")
        String taskName,
        @NotNull(message = "Monday hours are required")
        BigDecimal mondayHours,
        @NotNull(message = "Tuesday hours are required")
        BigDecimal tuesdayHours,
        @NotNull(message = "Wednesday hours are required")
        BigDecimal wednesdayHours,
        @NotNull(message = "Thursday hours are required")
        BigDecimal thursdayHours,
        @NotNull(message = "Friday hours are required")
        BigDecimal fridayHours,
        @NotNull(message = "Saturday hours are required")
        BigDecimal saturdayHours,
        @NotNull(message = "Sunday hours are required")
        BigDecimal sundayHours
) {
}
