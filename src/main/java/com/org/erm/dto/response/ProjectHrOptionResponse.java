package com.org.erm.dto.response;

public record ProjectHrOptionResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String roleName
) {
}
