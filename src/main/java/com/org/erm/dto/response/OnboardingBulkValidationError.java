package com.org.erm.dto.response;

public record OnboardingBulkValidationError(Integer rowNumber, String field, String message) {
}
