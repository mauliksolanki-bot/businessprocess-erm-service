package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ProjectAllocationStatus {
    PENDING_DM_APPROVAL("Pending DM Approval"),
    ACTIVE("Active"),
    REFER_BACK("Refer Back"),
    REJECTED("Rejected"),
    RELEASED("Released");

    private final String label;

    ProjectAllocationStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ProjectAllocationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(status -> status.label.equalsIgnoreCase(normalized)
                        || status.name().equalsIgnoreCase(normalized)
                        || status.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown allocation status: " + value));
    }
}
