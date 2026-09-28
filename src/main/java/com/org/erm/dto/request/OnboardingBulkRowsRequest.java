package com.org.erm.dto.request;

import java.util.List;

public record OnboardingBulkRowsRequest(List<OnboardingBulkRowRequest> rows) {
}
