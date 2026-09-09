package com.org.erm.dto.response;

public record ReportingManagerConfigResponse(
        Long id,
        Long roleId,
        String designationRoleName,
        String reportsToRoleName,
        Integer sortOrder,
        boolean active
) {
}
