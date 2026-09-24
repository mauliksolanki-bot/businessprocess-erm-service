package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * The designation Admin can optionally route an onboarding request to for an
 * additional (final) approval step after Admin's own approval.
 */
public enum OnboardingAdditionalApproverDesignation {
    SUPER_ADMIN("Super Admin", "ROLE_SUPER_ADMIN"),
    CHRO("CHRO", "ROLE_CHRO"),
    CEO("CEO", "ROLE_CEO"),
    CTO("CTO", "ROLE_CTO");

    private final String label;
    private final String authority;

    OnboardingAdditionalApproverDesignation(String label, String authority) {
        this.label = label;
        this.authority = authority;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    public String getAuthority() {
        return authority;
    }

    @JsonCreator
    public static OnboardingAdditionalApproverDesignation fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(designation -> designation.label.equalsIgnoreCase(normalized)
                        || designation.name().equalsIgnoreCase(normalized)
                        || designation.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown additional approver designation: " + value));
    }
}
