package com.org.erm.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RemoveRolesRequest(
        @NotEmpty(message = "At least one role must be specified")
        List<Long> roleIds
) {
}
