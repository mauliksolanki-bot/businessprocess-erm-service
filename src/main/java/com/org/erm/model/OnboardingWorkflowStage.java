package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OnboardingWorkflowStage {
    HR_SUBMITTED("HR Submitted"),
    HEAD_HR_APPROVED("Head HR Approved"),
    CHRO_APPROVED("CHRO Approved"),
    SUPER_ADMIN_APPROVED("Super Admin Approved"),
    REFER_BACK("Refer Back"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected");

    private final String label;

    OnboardingWorkflowStage(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static OnboardingWorkflowStage fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(stage -> stage.label.equalsIgnoreCase(normalized)
                        || stage.name().equalsIgnoreCase(normalized)
                        || stage.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow stage: " + value));
    }
}
