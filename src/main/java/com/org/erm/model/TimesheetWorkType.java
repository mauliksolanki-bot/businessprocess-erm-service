package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimesheetWorkType {
    BILLABLE("Billable"),
    NON_BILLABLE("Non-Billable");

    private final String label;

    TimesheetWorkType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static TimesheetWorkType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(type -> type.label.equalsIgnoreCase(normalized)
                        || type.name().equalsIgnoreCase(normalized)
                        || type.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown timesheet work type: " + value));
    }
}
