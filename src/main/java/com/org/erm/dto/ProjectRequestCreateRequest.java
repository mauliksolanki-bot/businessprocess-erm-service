package com.org.erm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectRequestCreateRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 3, max = 150, message = "Project name must be between 3 and 150 characters")
        String projectName,

        @NotBlank(message = "Project code is required")
        @Pattern(regexp = "^[A-Z0-9\\-]{3,30}$", message = "Project code must be 3-30 chars and only A-Z, 0-9, hyphen")
        String projectCode,

        @NotBlank(message = "Client name is required")
        @Size(min = 2, max = 150, message = "Client name must be between 2 and 150 characters")
        String clientName,

        @NotBlank(message = "Project type is required")
        @Size(max = 40, message = "Project type must be at most 40 characters")
        String projectType,

        @NotBlank(message = "Priority is required")
        @Size(max = 20, message = "Priority must be at most 20 characters")
        String priority,

        @NotNull(message = "Planned start date is required")
        LocalDate plannedStartDate,

        @NotNull(message = "Planned end date is required")
        LocalDate plannedEndDate,

        @NotNull(message = "Budget amount is required")
        @DecimalMin(value = "0.01", message = "Budget amount must be greater than zero")
        @Digits(integer = 13, fraction = 2, message = "Budget amount supports up to 13 digits and 2 decimals")
        BigDecimal budgetAmount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3,10}$", message = "Currency must be uppercase code, e.g. INR")
        String currency,

        @NotNull(message = "Delivery manager is required")
        Long deliveryManagerUserId,

        @NotNull(message = "Project owner is required")
        Long projectOwnerUserId,

        @NotNull(message = "Project director is required")
        Long projectDirectorUserId,

        @NotNull(message = "Project manager is required")
        Long projectManagerUserId,

        @NotBlank(message = "Project status is required")
        @Size(max = 30, message = "Project status must be at most 30 characters")
        String projectStatus,

        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
        String description,

        @Size(max = 1000, message = "Risk notes must be at most 1000 characters")
        String riskNotes,

        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
