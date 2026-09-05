package com.org.erm.dto.response;

public record SupportAssigneeOptionResponse(
        Long id,
        String username,
        String fullName,
        String email
) {
}
