package com.org.erm.dto.response;

import java.util.List;

public record OnboardingBulkValidationResponse(boolean valid, List<OnboardingBulkValidationError> errors) {
}
