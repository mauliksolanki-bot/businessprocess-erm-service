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

    public RoleAuditService(
            EmployeeService employeeService,
            ErmUserRepository ermUserRepository,
            ErmRoleRepository ermRoleRepository
    ) {
        this.employeeService = employeeService;
        this.ermUserRepository = ermUserRepository;
        this.ermRoleRepository = ermRoleRepository;
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
    public EmployeeResponse assignRoles(Long employeeId, AssignRolesRequest request) {
        if (request.roleIds() == null || request.roleIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one role must be selected");
        }

        ErmUser user = ermUserRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        Set<Long> requestedRoleIds = new LinkedHashSet<>(request.roleIds());
        List<ErmRole> rolesToAssign = new ArrayList<>();
        ermRoleRepository.findAllById(requestedRoleIds).forEach(rolesToAssign::add);
        if (rolesToAssign.size() != requestedRoleIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more roles were not found");
        }

        if (user.getPrimaryRoleId() == null && !user.getRoles().isEmpty()) {
            user.setPrimaryRoleId(user.getRoles().stream()
                    .map(ErmRole::getId)
                    .min(Long::compareTo)
                    .orElse(null));
        }

        if (user.getPrimaryRoleId() != null && requestedRoleIds.stream().noneMatch(roleId -> roleId.equals(user.getPrimaryRoleId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary role cannot be removed");
        }

        user.setRoles(new LinkedHashSet<>(rolesToAssign));
        ermUserRepository.save(user);
        return employeeService.getEmployeeById(employeeId);
    }
}
