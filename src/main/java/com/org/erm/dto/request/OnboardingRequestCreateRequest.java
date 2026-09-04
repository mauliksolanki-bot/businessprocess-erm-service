package com.org.erm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OnboardingRequestCreateRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Aadhaar card number is required")
        @Pattern(regexp = "^\\d{12}$", message = "Aadhaar card number must be exactly 12 digits")
        String aadhaarCardNumber,

        @NotBlank(message = "PAN card number is required")
        @Pattern(regexp = "(?i)^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN card number must be in valid format")
        String panCardNumber,

        @NotBlank(message = "Personal email address is required")
        @Email(message = "Personal email address must be valid")
        @Size(max = 150, message = "Personal email address must be at most 150 characters")
        String personalEmailAddress,

        @NotBlank(message = "Permanent address is required")
        @Size(max = 500, message = "Permanent address must be at most 500 characters")
        String permanentAddress,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9+()\\-\\s]{7,25}$", message = "Phone number must be valid")
        String phoneNumber,

        @NotBlank(message = "Designation is required")
        @Size(max = 100, message = "Designation must be at most 100 characters")
        String designationRoleName,

        @NotNull(message = "Reporting manager is required")
        Long reportingManagerUserId,

        @Size(max = 255, message = "Education qualification must be at most 255 characters")
        String educationQualification,

        @Size(max = 500, message = "Comment must be at most 500 characters")
        String comment
) {
}
