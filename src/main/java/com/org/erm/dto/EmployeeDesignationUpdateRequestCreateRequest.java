package com.org.erm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeDesignationUpdateRequestCreateRequest(
        @NotNull(message = "Employee is required")
        Long employeeUserId,

        @NotBlank(message = "Designation is required")
        @Size(max = 100, message = "Designation must be at most 100 characters")
        String designationRoleName,

        @NotNull(message = "Reporting manager is required")
        Long reportingManagerUserId,

        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
