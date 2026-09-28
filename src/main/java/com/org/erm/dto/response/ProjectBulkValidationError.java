package com.org.erm.dto.response;

public record ProjectBulkValidationError(Integer rowNumber, String field, String message) {
}
