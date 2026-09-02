package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OnboardingInterviewStage {
    STARTED("Started"),
    ON_HOLD("On-Hold"),
    PENDING("Pending"),
    COMPLETED("Completed"),
    REJECTED("Rejected");

    private final String label;

    OnboardingInterviewStage(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static OnboardingInterviewStage fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(stage -> stage.label.equalsIgnoreCase(normalized)
                        || stage.name().equalsIgnoreCase(normalized)
                        || stage.name().equalsIgnoreCase(normalized.replace('-', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown interview stage: " + value));
    }
}