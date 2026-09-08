package com.org.erm.controller;

import com.org.erm.dto.request.TimesheetActionRequest;
import com.org.erm.dto.request.TimesheetSubmitRequest;
import com.org.erm.dto.response.TimesheetApproverVisibilityResponse;
import com.org.erm.dto.response.TimesheetResponse;
import com.org.erm.service.TimesheetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/timesheets")
@PreAuthorize("isAuthenticated()")
public class TimesheetController {

    private final TimesheetService timesheetService;

    public TimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @PostMapping
    public ResponseEntity<TimesheetResponse> submit(@Valid @RequestBody TimesheetSubmitRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timesheetService.submit(request, authentication.getName()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TimesheetResponse>> myTimesheets(Authentication authentication) {
        return ResponseEntity.ok(timesheetService.myTimesheets(authentication.getName()));
    }

    @GetMapping("/approvals")
    public ResponseEntity<List<TimesheetResponse>> approvals(Authentication authentication) {
        return ResponseEntity.ok(timesheetService.approvals(authentication.getName()));
    }

    @GetMapping("/approver-visibility")
    public ResponseEntity<TimesheetApproverVisibilityResponse> approverVisibility(Authentication authentication) {
        return ResponseEntity.ok(timesheetService.approverVisibility(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimesheetResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(timesheetService.getById(id, authentication.getName()));
    }

    @PatchMapping("/{id}/actions")
    public ResponseEntity<TimesheetResponse> action(@PathVariable Long id,
                                                    @Valid @RequestBody TimesheetActionRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(timesheetService.action(id, request, authentication.getName()));
    }
}
