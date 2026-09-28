package com.org.erm.dto.response;

import java.util.List;

public record HrOverviewDashboardResponse(
        long totalActiveUsers,
        long employeesMappedToHrbp,
        long employeesWithoutHrbp,
        List<DesignationUserCount> designationCounts,
        List<HrBpEmployeeCount> hrbpEmployeeCounts
) {
    public record DesignationUserCount(
            String designation,
            long userCount
    ) {
    }

    public record HrBpEmployeeCount(
            Long userId,
            String fullName,
            String username,
            String designation,
            long employeeCount
    ) {
    }
}
