package com.org.erm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeProfileUpdateRequestCreateRequest(
        @NotNull(message = "Employee is required")
        Long employeeUserId,

        @NotBlank(message = "Department is required")
        @Size(max = 100, message = "Department must not exceed 100 characters")
        String department,

        @NotBlank(message = "Employment status is required")
        @Size(max = 30, message = "Employment status must not exceed 30 characters")
        String employmentStatus,

        @NotBlank(message = "Designation is required")
        @Size(max = 100, message = "Designation must not exceed 100 characters")
        String designationRoleName,

        @NotNull(message = "Reporting manager is required")
        Long reportingManagerUserId,

        Long replacementTeamLeadUserId,

        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
