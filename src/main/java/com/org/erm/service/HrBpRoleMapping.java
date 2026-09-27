package com.org.erm.service;

import java.util.Locale;
import java.util.Map;

public final class HrBpRoleMapping {

    private static final Map<String, String> ASSOCIATED_ROLE_BY_DESIGNATION = Map.ofEntries(
            Map.entry("super admin", "Senior HR"),
            Map.entry("admin", "Senior HR"),
            Map.entry("ceo", "HR Head"),
            Map.entry("cfo", "HR Head"),
            Map.entry("cto", "HR Head"),
            Map.entry("chro", "HR Head"),
            Map.entry("hr head", "CHRO"),
            Map.entry("senior hr", "HR Head"),
            Map.entry("junior hr", "Senior HR"),
            Map.entry("project owner", "Senior HR"),
            Map.entry("program manager", "Senior HR"),
            Map.entry("delivery manager", "Senior HR"),
            Map.entry("project manager", "Senior HR"),
            Map.entry("team lead", "Senior HR"),
            Map.entry("employee", "Junior HR"),
            Map.entry("intern", "Junior HR"),
            Map.entry("application support specialist", "Junior HR"),
            Map.entry("it security", "Junior HR"),
            Map.entry("it support lead", "Senior HR"),
            Map.entry("it support manager", "Senior HR"),
            Map.entry("director", "HR Head")
    );

    private HrBpRoleMapping() {
    }

    public static String associatedRoleFor(String designationRoleName) {
        if (designationRoleName == null || designationRoleName.isBlank()) {
            return "Junior HR";
        }
        return ASSOCIATED_ROLE_BY_DESIGNATION.getOrDefault(
                designationRoleName.trim().toLowerCase(Locale.ROOT), "Junior HR");
    }
}
