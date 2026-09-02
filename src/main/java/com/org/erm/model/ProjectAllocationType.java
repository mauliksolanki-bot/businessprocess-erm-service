package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ProjectAllocationType {
    BILLABLE("Billable"),
    BUFFER("Buffer"),
    SHADOW("Shadow"),
    TRAINING("Training"),
    INTERNAL("Internal");

    private final String label;

    ProjectAllocationType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ProjectAllocationType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(type -> type.label.equalsIgnoreCase(normalized)
                        || type.name().equalsIgnoreCase(normalized)
                        || type.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown allocation type: " + value));
    }
}
