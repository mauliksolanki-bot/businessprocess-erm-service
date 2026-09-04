package com.org.erm.controller;

import com.org.erm.dto.request.LeavePolicyCreateRequest;
import com.org.erm.dto.response.LeavePolicyResponse;
import com.org.erm.dto.request.LeavePolicyUpdateRequest;
import com.org.erm.service.LeavePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leave-policies")
@Tag(name = "Leave Policies", description = "Leave policy configuration by HR Head")
@PreAuthorize("hasAuthority('ROLE_HR_HEAD')")
public class LeavePolicyController {

    private final LeavePolicyService leavePolicyService;

    public LeavePolicyController(LeavePolicyService leavePolicyService) {
        this.leavePolicyService = leavePolicyService;
    }

    @GetMapping
    @Operation(summary = "List leave policies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave policies fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<LeavePolicyResponse>> list() {
        return ResponseEntity.ok(leavePolicyService.listPolicies());
    }

    @PostMapping
    @Operation(summary = "Create leave policy")
    public ResponseEntity<LeavePolicyResponse> create(@Valid @RequestBody LeavePolicyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leavePolicyService.createPolicy(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update leave policy")
    public ResponseEntity<LeavePolicyResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody LeavePolicyUpdateRequest request) {
        return ResponseEntity.ok(leavePolicyService.updatePolicy(id, request));
    }
}
