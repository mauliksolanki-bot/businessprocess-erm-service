package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AttendanceTimesheetStatus {
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    AttendanceTimesheetStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static AttendanceTimesheetStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(status -> status.label.equalsIgnoreCase(normalized)
                        || status.name().equalsIgnoreCase(normalized)
                        || status.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown attendance timesheet status: " + value));
    }
}
