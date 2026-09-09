package com.org.erm.dto.response;

public record RoleConfigResponse(
        Long id,
        String roleName,
        String roleDescription,
        long assignedUserCount,
        boolean roleNameEditable
) {
}
