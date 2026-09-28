package com.org.erm.dto.request;

public record OnboardingBulkRowRequest(
        Integer rowNumber,
        String firstName,
        String lastName,
        String aadhaarCardNumber,
        String panCardNumber,
        String personalEmailAddress,
        String permanentAddress,
        String phoneNumber,
        String designationRoleName,
        String reportingManagerUsername,
        String hrbpUsername,
        String educationQualification,
        String comment
) {
}
