package com.org.erm.dto.response;

import java.util.List;

public record OrganizationConfigResponse(
        List<RoleSummaryResponse> roles,
        List<ReportingManagerConfigResponse> reportingManagerConfigs,
        List<RoleConfigResponse> roleConfigs
) {
}
