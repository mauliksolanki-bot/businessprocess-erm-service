package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AttendanceDecision {
    APPROVE("Approve"),
    REJECT("Reject");

    private final String label;

    AttendanceDecision(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static AttendanceDecision fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(decision -> decision.label.equalsIgnoreCase(normalized)
                        || decision.name().equalsIgnoreCase(normalized)
                        || decision.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown attendance decision: " + value));
    }
}
