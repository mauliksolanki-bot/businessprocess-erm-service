package com.org.erm.dto.response;

public record DashboardSummaryResponse(
        long totalEmployees,
        long openOnboardingRequests,
        long openEmployeeDataRequests
) {
}
