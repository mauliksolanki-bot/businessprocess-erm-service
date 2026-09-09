package com.org.erm.controller;

import com.org.erm.dto.request.RoleConfigRequest;
import com.org.erm.dto.request.ReportingManagerConfigRequest;
import com.org.erm.dto.response.OrganizationConfigResponse;
import com.org.erm.dto.response.RoleConfigResponse;
import com.org.erm.dto.response.ReportingManagerConfigResponse;
import com.org.erm.service.OrganizationConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization-config")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class OrganizationConfigController {

    private final OrganizationConfigService organizationConfigService;

    public OrganizationConfigController(OrganizationConfigService organizationConfigService) {
        this.organizationConfigService = organizationConfigService;
    }

    @GetMapping("/reporting-manager-configs")
    public ResponseEntity<OrganizationConfigResponse> getReportingManagerConfigs() {
        return ResponseEntity.ok(organizationConfigService.getReportingManagerConfig());
    }

    @PostMapping("/reporting-manager-configs")
    public ResponseEntity<ReportingManagerConfigResponse> createReportingManagerConfig(@Valid @RequestBody ReportingManagerConfigRequest request) {
        return ResponseEntity.ok(organizationConfigService.createReportingManagerConfig(request));
    }

    @PutMapping("/reporting-manager-configs/{configId}")
    public ResponseEntity<ReportingManagerConfigResponse> updateReportingManagerConfig(
            @PathVariable Long configId,
            @Valid @RequestBody ReportingManagerConfigRequest request
    ) {
        return ResponseEntity.ok(organizationConfigService.updateReportingManagerConfig(configId, request));
    }

    @GetMapping("/roles/{roleId}")
    public ResponseEntity<RoleConfigResponse> getRoleConfig(@PathVariable Long roleId) {
        return ResponseEntity.ok(organizationConfigService.getRoleConfig(roleId));
    }

    @PostMapping("/roles")
    public ResponseEntity<RoleConfigResponse> createRoleConfig(@Valid @RequestBody RoleConfigRequest request) {
        return ResponseEntity.ok(organizationConfigService.createRoleConfig(request));
    }

    @PutMapping("/roles/{roleId}")
    public ResponseEntity<RoleConfigResponse> updateRoleConfig(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleConfigRequest request
    ) {
        return ResponseEntity.ok(organizationConfigService.updateRoleConfig(roleId, request));
    }
}
