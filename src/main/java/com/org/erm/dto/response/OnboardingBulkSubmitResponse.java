package com.org.erm.dto.response;

import java.util.List;

public record OnboardingBulkSubmitResponse(
        boolean submitted,
        List<OnboardingBulkValidationError> errors,
        List<Long> createdRequestIds
) {
}
