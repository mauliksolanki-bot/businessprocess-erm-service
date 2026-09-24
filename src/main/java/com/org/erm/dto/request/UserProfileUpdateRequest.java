package com.org.erm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Email(message = "Personal email must be valid")
        @Size(max = 150, message = "Personal email must not exceed 150 characters")
        String personalEmailAddress,

        @Pattern(regexp = "^$|^[0-9+\\-() ]{7,25}$", message = "Phone number must be a valid phone number")
        @Size(max = 25, message = "Phone number must not exceed 25 characters")
        String phoneNumber,

        @Size(max = 255, message = "Education details must not exceed 255 characters")
        String educationQualification
) {
}
