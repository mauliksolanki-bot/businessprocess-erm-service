package com.org.erm.controller;

import com.org.erm.dto.DashboardSummaryResponse;
import com.org.erm.dto.TeamLeadDashboardResponse;
import com.org.erm.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Live dashboard summary counts")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_CHRO','ROLE_HR_HEAD','ROLE_SENIOR_HR','ROLE_TEAM_LEAD','ROLE_IT_SUPPORT_LEAD')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get live dashboard summary counts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard summary fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<DashboardSummaryResponse> summary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/team-lead")
    @Operation(summary = "Get team lead dashboard details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team lead dashboard fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyAuthority('ROLE_TEAM_LEAD','ROLE_IT_SUPPORT_LEAD','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
    public ResponseEntity<TeamLeadDashboardResponse> teamLeadSummary(java.security.Principal principal) {
        return ResponseEntity.ok(dashboardService.getTeamLeadDashboard(principal.getName()));
    }
}
