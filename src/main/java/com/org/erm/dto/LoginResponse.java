package com.org.erm.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String username,
        List<String> roles
) {
}
