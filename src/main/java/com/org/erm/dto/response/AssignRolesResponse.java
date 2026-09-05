package com.org.erm.dto.response;

import java.util.List;

public record AssignRolesResponse(
        EmployeeResponse employee,
        List<Long> createdRoleIds,
        List<Long> existingRoleIds
) {
}
