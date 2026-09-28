package com.org.erm.dto.response;

import java.util.List;

public record OnboardingBulkTemplateDesignationResponse(
        String designationRoleName,
        String reportingManagerRoleName,
        String hrbpRoleName,
        List<String> reportingManagerUsernames,
        List<String> hrbpUsernames
) {
}
