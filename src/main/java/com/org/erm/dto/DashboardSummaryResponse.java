package com.org.erm.dto;

public record DashboardSummaryResponse(
        long totalEmployees,
        long openOnboardingRequests,
        long openEmployeeDataRequests
) {
}
