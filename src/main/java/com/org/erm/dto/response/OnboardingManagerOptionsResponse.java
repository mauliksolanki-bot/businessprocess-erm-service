package com.org.erm.dto.response;

import java.util.List;

public record OnboardingManagerOptionsResponse(
        String designationRoleName,
        String managerRoleName,
        List<OnboardingManagerOptionResponse> managers
) {
}
