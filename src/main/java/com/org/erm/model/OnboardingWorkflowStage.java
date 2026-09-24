package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OnboardingWorkflowStage {
    HR_SUBMITTED("HR Submitted"),
    HEAD_HR_APPROVED("Head HR Approved"),
    ADDITIONAL_APPROVAL_PENDING("Additional Approval Pending"),
    ADMIN_APPROVED("Admin Approved"),
    ADDITIONAL_APPROVAL_APPROVED("Additional Approval Approved"),
    // Retained for the (separate, unrelated) employee profile-update-request approval workflow,
    // which reuses this enum and still has its own 4-stage HR -> Head HR -> CHRO -> Super Admin chain.
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

    public boolean isClosed() {
        return this == ADMIN_APPROVED || this == ADDITIONAL_APPROVAL_APPROVED
                || this == REJECTED || this == CANCELLED;
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
