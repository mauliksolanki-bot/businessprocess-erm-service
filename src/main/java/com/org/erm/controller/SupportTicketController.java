package com.org.erm.controller;

import com.org.erm.dto.response.SupportCatalogResponse;
import com.org.erm.dto.response.SupportQueueSummaryResponse;
import com.org.erm.dto.request.SupportTicketAssignRequest;
import com.org.erm.dto.request.SupportTicketCommentRequest;
import com.org.erm.dto.request.SupportTicketCreateRequest;
import com.org.erm.dto.response.SupportTicketResponse;
import com.org.erm.dto.request.SupportTicketStatusRequest;
import com.org.erm.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@Tag(name = "Support Tickets", description = "Support ticket, incident, and security incident workflow")
@PreAuthorize("isAuthenticated()")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping("/tickets")
    @Operation(summary = "Create a support ticket")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Support ticket created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload")
    })
    public ResponseEntity<SupportTicketResponse> create(@Valid @RequestBody SupportTicketCreateRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.create(request, authentication));
    }

    @GetMapping("/tickets")
    @Operation(summary = "List support tickets")
    public ResponseEntity<List<SupportTicketResponse>> list(
            @RequestParam(name = "scope", required = false) String scope,
            @RequestParam(name = "queueCode", required = false) String queueCode,
            @RequestParam(name = "status", required = false) String status,
            Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.list(scope, queueCode, status, authentication));
    }

    @GetMapping("/tickets/{id}")
    @Operation(summary = "Get support ticket by id")
    public ResponseEntity<SupportTicketResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.getById(id, authentication));
    }

    @PostMapping("/tickets/{id}/comments")
    @Operation(summary = "Add a comment to support ticket")
    public ResponseEntity<SupportTicketResponse> addComment(@PathVariable Long id,
                                                            @Valid @RequestBody SupportTicketCommentRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.addComment(id, request, authentication));
    }

    @PatchMapping("/tickets/{id}/status")
    @Operation(summary = "Update support ticket status")
    public ResponseEntity<SupportTicketResponse> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody SupportTicketStatusRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.updateStatus(id, request, authentication));
    }

    @PatchMapping("/tickets/{id}/assign")
    @Operation(summary = "Assign or reassign support ticket")
    public ResponseEntity<SupportTicketResponse> assign(@PathVariable Long id,
                                                        @Valid @RequestBody SupportTicketAssignRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.assign(id, request, authentication));
    }

    @GetMapping("/catalog/options")
    @Operation(summary = "Get support catalog options")
    public ResponseEntity<SupportCatalogResponse> catalog(Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.catalog(authentication));
    }

    @GetMapping("/workbench/queues")
    @Operation(summary = "Get support workbench queues for current user")
    public ResponseEntity<List<SupportQueueSummaryResponse>> workbenchQueues(Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.workbenchQueues(authentication));
    }

    @GetMapping("/workbench/tickets")
    @Operation(summary = "Get support workbench tickets for a queue")
    public ResponseEntity<List<SupportTicketResponse>> workbenchTickets(@RequestParam(name = "queueCode") String queueCode,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(supportTicketService.workbenchTickets(queueCode, authentication));
    }
}
