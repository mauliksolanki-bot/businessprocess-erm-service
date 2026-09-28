package com.org.erm.dto.response;

import java.util.List;

public record ProjectBulkValidationResponse(boolean valid, List<ProjectBulkValidationError> errors) {
}
