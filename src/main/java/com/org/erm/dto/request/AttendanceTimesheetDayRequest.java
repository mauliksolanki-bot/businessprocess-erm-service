package com.org.erm.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceTimesheetDayRequest(
        @NotNull(message = "Work date is required")
        LocalDate workDate,
        @NotNull(message = "Billable hours are required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Billable hours must be zero or positive")
        BigDecimal billableHours,
        @NotNull(message = "Non-billable hours are required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Non-billable hours must be zero or positive")
        BigDecimal nonBillableHours,
        Long billableProjectAllocationId,
        Long nonBillableProjectAllocationId
) {
}
