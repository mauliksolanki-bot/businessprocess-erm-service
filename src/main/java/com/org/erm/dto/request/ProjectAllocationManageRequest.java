package com.org.erm.dto.request;

import com.org.erm.model.ProjectAllocationManageAction;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectAllocationManageRequest(
        @NotNull(message = "Manage action is required")
        ProjectAllocationManageAction action,

        @DecimalMin(value = "0.01", message = "Allocation percent must be greater than zero")
        @DecimalMax(value = "100.00", message = "Allocation percent must be at most 100")
        BigDecimal allocationPercent,

        LocalDate endDate,

        @NotBlank(message = "Comment is required")
        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
