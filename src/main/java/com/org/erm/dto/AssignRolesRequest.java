package com.org.erm.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignRolesRequest(
        @NotEmpty(message = "At least one role must be selected")
        List<Long> roleIds
) {
}
