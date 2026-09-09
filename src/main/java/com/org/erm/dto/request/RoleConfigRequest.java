package com.org.erm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleConfigRequest(
        @NotBlank(message = "Role name is required")
        @Size(max = 100, message = "Role name must be 100 characters or less")
        String roleName,

        @Size(max = 255, message = "Role description must be 255 characters or less")
        String roleDescription
) {
}
