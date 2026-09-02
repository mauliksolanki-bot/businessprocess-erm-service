package com.org.erm.dto;

import java.util.List;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String designation,
        List<String> roles
) {
}
