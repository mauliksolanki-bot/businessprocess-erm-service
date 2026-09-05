package com.org.erm.dto.response;

import java.util.List;

public record RemoveRolesResponse(
        EmployeeResponse employee,
        List<Long> removedRoleIds,
        List<Long> notAssignedRoleIds
) {
}
