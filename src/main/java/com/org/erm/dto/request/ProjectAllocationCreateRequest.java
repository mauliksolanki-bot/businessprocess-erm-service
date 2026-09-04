package com.org.erm.dto.request;

import com.org.erm.model.ProjectAllocationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectAllocationCreateRequest(
        @NotNull(message = "Project request is required")
        Long projectRequestId,

        @NotNull(message = "Employee is required")
        Long employeeUserId,

        @NotNull(message = "Allocation type is required")
        ProjectAllocationType allocationType,

        @NotNull(message = "Allocation percent is required")
        @DecimalMin(value = "0.01", message = "Allocation percent must be greater than zero")
        @DecimalMax(value = "100.00", message = "Allocation percent must be at most 100")
        BigDecimal allocationPercent,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
