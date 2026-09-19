package com.org.erm.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AttendanceTimesheetUpsertRequest(
        @NotNull(message = "Week start date is required")
        LocalDate weekStartDate,
        @Valid
        @NotNull(message = "Timesheet days are required")
        @Size(min = 7, max = 7, message = "Weekly timesheet must include 7 days")
        List<AttendanceTimesheetDayRequest> days
) {
}
