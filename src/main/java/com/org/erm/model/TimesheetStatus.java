package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimesheetStatus {
    PENDING_MANAGER_APPROVAL("Pending Manager Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    TimesheetStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static TimesheetStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(status -> status.label.equalsIgnoreCase(normalized)
                        || status.name().equalsIgnoreCase(normalized)
                        || status.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown timesheet status: " + value));
    }
}
