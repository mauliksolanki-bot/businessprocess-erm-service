package com.org.erm.dto.response;

import java.util.List;

public record ProjectBulkSubmitResponse(boolean submitted, List<ProjectBulkValidationError> errors, List<Long> createdRequestIds) {
}
