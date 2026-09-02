package com.org.erm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ProjectWorkflowStage {
    PM_SUBMITTED("PM Submitted"),
    DELIVERY_MANAGER_APPROVED("Delivery Manager Approved"),
    PROJECT_OWNER_APPROVED("Project Owner Approved"),
    DIRECTOR_APPROVED("Director Approved"),
    CTO_APPROVED("CTO Approved"),
    SUPER_ADMIN_APPROVED("Super Admin Approved"),
    REFER_BACK("Refer Back"),
    REJECTED("Rejected");

    private final String label;

    ProjectWorkflowStage(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ProjectWorkflowStage fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(stage -> stage.label.equalsIgnoreCase(normalized)
                        || stage.name().equalsIgnoreCase(normalized)
                        || stage.name().equalsIgnoreCase(normalized.replace('-', '_').replace(' ', '_')))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown project workflow stage: " + value));
    }
}
