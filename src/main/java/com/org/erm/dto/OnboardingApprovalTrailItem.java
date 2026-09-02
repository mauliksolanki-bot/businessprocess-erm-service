package com.org.erm.dto;

import java.time.LocalDateTime;

public record OnboardingApprovalTrailItem(
        String step,
        String actor,
        String decision,
        String comment,
        LocalDateTime actionAt
) {
}
