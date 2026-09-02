package com.org.erm.security;

import java.util.Locale;

public final class RoleAuthorityMapper {

    private RoleAuthorityMapper() {
    }

    public static String toAuthority(String roleName) {
        String normalized = roleName
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_");
        return "ROLE_" + normalized;
    }
}
