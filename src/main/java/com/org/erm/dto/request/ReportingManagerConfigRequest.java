package com.org.erm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReportingManagerConfigRequest(
        @NotBlank(message = "Designation role is required")
        String designationRoleName,

        @NotBlank(message = "Reporting manager role is required")
        String reportsToRoleName
) {
}
