package com.org.erm.service;

import com.org.erm.dto.request.AssignRolesRequest;
import com.org.erm.dto.response.EmployeeResponse;
import com.org.erm.dto.response.PagedResponse;
import com.org.erm.dto.response.RoleSummaryResponse;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.repository.ErmRoleRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleAuditService {

    private final EmployeeService employeeService;
    private final ErmUserRepository ermUserRepository;
    private final ErmRoleRepository ermRoleRepository;
    private final EmployeeIdService employeeIdService;
    private final EmployeeRoleReferenceService employeeRoleReferenceService;

    public RoleAuditService(
            EmployeeService employeeService,
            ErmUserRepository ermUserRepository,
            ErmRoleRepository ermRoleRepository,
            EmployeeIdService employeeIdService,
            EmployeeRoleReferenceService employeeRoleReferenceService
    ) {
        this.employeeService = employeeService;
        this.ermUserRepository = ermUserRepository;
        this.ermRoleRepository = ermRoleRepository;
        this.employeeIdService = employeeIdService;
        this.employeeRoleReferenceService = employeeRoleReferenceService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> searchEmployees(String employeeName, String roleName, String department, String status, int page, int size) {
        return employeeService.searchEmployees(employeeName, roleName, department, status, page, size);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long employeeId) {
        return employeeService.getEmployeeById(employeeId);
    }

    @Transactional(readOnly = true)
    public List<RoleSummaryResponse> getAllRoles() {
        return ermRoleRepository.findAllByOrderByNameAsc().stream()
                .map(role -> new RoleSummaryResponse(role.getId(), role.getName(), role.getDescription()))
                .toList();
    }

    @Transactional
    public com.org.erm.dto.response.AssignRolesResponse assignRoles(Long employeeId, AssignRolesRequest request) {
        if (request.roleIds() == null || request.roleIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role must be selected");
        }

        ErmUser user = ermUserRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        // maintain insertion order and uniqueness
        Set<Long> requestedRoleIds = new LinkedHashSet<>(request.roleIds());

        // validate roles exist
        List<ErmRole> foundRoles = new ArrayList<>();
        ermRoleRepository.findAllById(requestedRoleIds).forEach(foundRoles::add);
        if (foundRoles.size() != requestedRoleIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more roles were not found");
        }

        // compute already assigned role ids
        Set<Long> existingAssignedIds = user.getRoles().stream()
                .map(ErmRole::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<Long> existingRoleIds = new ArrayList<>();
        List<Long> createdRoleIds = new ArrayList<>();

        // determine which roles are new vs existing
        for (ErmRole role : foundRoles) {
            if (existingAssignedIds.contains(role.getId())) {
                existingRoleIds.add(role.getId());
            } else {
                createdRoleIds.add(role.getId());
            }
        }

        // add new roles to the user's role set (keep existing roles)
        Set<ErmRole> updatedRoles = new LinkedHashSet<>(user.getRoles());
        for (ErmRole role : foundRoles) {
            updatedRoles.add(role);
        }

        // primary role checks: if no primary set, preserve current behavior
        if (user.getPrimaryRoleId() == null && !updatedRoles.isEmpty()) {
            user.setPrimaryRoleId(updatedRoles.stream()
                    .map(ErmRole::getId)
                    .min(Long::compareTo)
                    .orElse(null));
        }

        // ensure primary role is not removed by this flow (we're only adding, so safe)
        user.setRoles(updatedRoles);
        employeeIdService.refreshForPrimaryRole(user);
        ermUserRepository.save(user);
        employeeRoleReferenceService.syncEmployeeIds(user.getId());

        EmployeeResponse employeeResponse = employeeService.getEmployeeById(employeeId);
        return new com.org.erm.dto.response.AssignRolesResponse(employeeResponse, createdRoleIds, existingRoleIds);
    }

    @Transactional
    public com.org.erm.dto.response.RemoveRolesResponse removeRoles(Long employeeId, com.org.erm.dto.request.RemoveRolesRequest request) {
        if (request.roleIds() == null || request.roleIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role must be specified");
        }

        ErmUser user = ermUserRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        Set<Long> requestedRoleIds = new LinkedHashSet<>(request.roleIds());

        List<ErmRole> foundRoles = new ArrayList<>();
        ermRoleRepository.findAllById(requestedRoleIds).forEach(foundRoles::add);
        if (foundRoles.size() != requestedRoleIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more roles were not found");
        }

        Set<Long> currentAssignedIds = user.getRoles().stream().map(ErmRole::getId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<Long> removedRoleIds = new ArrayList<>();
        List<Long> notAssignedRoleIds = new ArrayList<>();

        for (ErmRole role : foundRoles) {
            Long id = role.getId();
            if (!currentAssignedIds.contains(id)) {
                notAssignedRoleIds.add(id);
                continue;
            }
            if (user.getPrimaryRoleId() != null && id.equals(user.getPrimaryRoleId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary role cannot be removed");
            }
            removedRoleIds.add(id);
        }

        // remove roles
        Set<ErmRole> updatedRoles = new LinkedHashSet<>(user.getRoles());
        updatedRoles.removeIf(r -> removedRoleIds.contains(r.getId()));

        // if primary role missing, shouldn't happen because we guard above
        user.setRoles(updatedRoles);
        ermUserRepository.save(user);

        EmployeeResponse employeeResponse = employeeService.getEmployeeById(employeeId);
        return new com.org.erm.dto.response.RemoveRolesResponse(employeeResponse, removedRoleIds, notAssignedRoleIds);
    }
}
