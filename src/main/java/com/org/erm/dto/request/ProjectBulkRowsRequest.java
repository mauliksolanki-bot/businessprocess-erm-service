package com.org.erm.dto.request;

import java.util.List;

public record ProjectBulkRowsRequest(List<ProjectBulkRowRequest> rows) {
}
