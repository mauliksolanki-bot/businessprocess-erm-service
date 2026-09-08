package com.org.erm.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TimesheetSubmitRequest(
        @NotNull(message = "Week start date is required")
        LocalDate weekStartDate,
        String submissionComment,
        @Valid
        @Size(min = 1, message = "At least one timesheet row is required")
        List<TimesheetEntryRequest> rows
) {
}
