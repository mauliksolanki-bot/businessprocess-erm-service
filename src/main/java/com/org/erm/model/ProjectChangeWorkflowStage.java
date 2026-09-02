package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ProjectChangeWorkflowStage {
    PENDING_DELIVERY_MANAGER_APPROVAL("Pending Director Approval"),
    PENDING_PROJECT_OWNER_APPROVAL("Pending CTO Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    ProjectChangeWorkflowStage(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ProjectChangeWorkflowStage fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(stage -> stage.label.equalsIgnoreCase(normalized)
                        || stage.name().equalsIgnoreCase(normalized)
                        || stage.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown project change workflow stage: " + value));
    }
}
