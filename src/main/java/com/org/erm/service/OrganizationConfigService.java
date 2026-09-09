package com.org.erm.service;

import com.org.erm.dto.request.RoleConfigRequest;
import com.org.erm.dto.request.ReportingManagerConfigRequest;
import com.org.erm.dto.response.OrganizationConfigResponse;
import com.org.erm.dto.response.RoleConfigResponse;
import com.org.erm.dto.response.ReportingManagerConfigResponse;
import com.org.erm.dto.response.RoleSummaryResponse;
import com.org.erm.model.ErmDesignationHierarchy;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.repository.ErmDesignationHierarchyRepository;
import com.org.erm.repository.ErmRoleRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OrganizationConfigService {

    private final ErmRoleRepository ermRoleRepository;
    private final ErmDesignationHierarchyRepository designationHierarchyRepository;
    private final ErmUserRepository ermUserRepository;

    public OrganizationConfigService(ErmRoleRepository ermRoleRepository,
                                     ErmDesignationHierarchyRepository designationHierarchyRepository,
                                     ErmUserRepository ermUserRepository) {
        this.ermRoleRepository = ermRoleRepository;
        this.designationHierarchyRepository = designationHierarchyRepository;
        this.ermUserRepository = ermUserRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationConfigResponse getReportingManagerConfig() {
        List<ErmRole> allRoles = ermRoleRepository.findAllByOrderByNameAsc();
        List<RoleSummaryResponse> roles = allRoles.stream()
                .map(role -> new RoleSummaryResponse(role.getId(), role.getName(), role.getDescription()))
                .toList();

        Map<String, ErmDesignationHierarchy> configsByRole = designationHierarchyRepository.findAllByActiveTrueOrderBySortOrderAscDesignationRoleNameAsc().stream()
                .collect(java.util.stream.Collectors.toMap(
                        hierarchy -> normalize(hierarchy.getDesignationRoleName()),
                        hierarchy -> hierarchy,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ReportingManagerConfigResponse> configs = roles.stream()
                .map(role -> {
                    ErmDesignationHierarchy hierarchy = configsByRole.get(normalize(role.name()));
                    return new ReportingManagerConfigResponse(
                            hierarchy != null ? hierarchy.getId() : null,
                            role.id(),
                            role.name(),
                            hierarchy != null ? hierarchy.getReportsToRoleName() : null,
                            hierarchy != null ? hierarchy.getSortOrder() : null,
                            hierarchy != null && hierarchy.isActive()
                    );
                })
                .toList();

        List<RoleConfigResponse> roleConfigs = allRoles.stream()
                .map(this::toRoleConfigResponse)
                .toList();

        return new OrganizationConfigResponse(roles, configs, roleConfigs);
    }

    @Transactional
    public ReportingManagerConfigResponse createReportingManagerConfig(ReportingManagerConfigRequest request) {
        String designationRoleName = normalizeRequired(request.designationRoleName(), "Designation role is required");
        String reportsToRoleName = normalizeRequired(request.reportsToRoleName(), "Reporting manager role is required");
        ensureDistinctRoles(designationRoleName, reportsToRoleName);
        validateRoleExists(designationRoleName, "Designation role not found");
        validateRoleExists(reportsToRoleName, "Reporting manager role not found");

        if (designationHierarchyRepository.findByDesignationRoleNameIgnoreCaseAndActiveTrue(designationRoleName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reporting manager config already exists for this role");
        }

        int nextSortOrder = designationHierarchyRepository.findAll().stream()
                .map(ErmDesignationHierarchy::getSortOrder)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        ErmDesignationHierarchy hierarchy = new ErmDesignationHierarchy();
        hierarchy.setDesignationRoleName(designationRoleName);
        hierarchy.setReportsToRoleName(reportsToRoleName);
        hierarchy.setSortOrder(nextSortOrder);
        hierarchy.setActive(true);
        hierarchy = designationHierarchyRepository.save(hierarchy);
        return toResponse(hierarchy);
    }

    @Transactional
    public ReportingManagerConfigResponse updateReportingManagerConfig(Long configId, ReportingManagerConfigRequest request) {
        ErmDesignationHierarchy hierarchy = designationHierarchyRepository.findById(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporting manager config not found"));

        String reportsToRoleName = normalizeRequired(request.reportsToRoleName(), "Reporting manager role is required");
        validateRoleExists(hierarchy.getDesignationRoleName(), "Designation role not found");
        validateRoleExists(reportsToRoleName, "Reporting manager role not found");
        ensureDistinctRoles(hierarchy.getDesignationRoleName(), reportsToRoleName);

        hierarchy.setReportsToRoleName(reportsToRoleName);
        hierarchy.setActive(true);
        hierarchy = designationHierarchyRepository.save(hierarchy);
        return toResponse(hierarchy);
    }

    @Transactional(readOnly = true)
    public RoleConfigResponse getRoleConfig(Long roleId) {
        ErmRole role = ermRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        return toRoleConfigResponse(role);
    }

    @Transactional
    public RoleConfigResponse createRoleConfig(RoleConfigRequest request) {
        String roleName = normalizeRequired(request.roleName(), "Role name is required");
        String roleDescription = normalizeOptional(request.roleDescription());

        if (ermRoleRepository.existsByNameIgnoreCase(roleName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
        }

        ErmRole role = new ErmRole();
        role.setName(roleName);
        role.setDescription(roleDescription);
        role.setSystem(false);
        role = ermRoleRepository.save(role);
        return toRoleConfigResponse(role);
    }

    @Transactional
    public RoleConfigResponse updateRoleConfig(Long roleId, RoleConfigRequest request) {
        ErmRole role = ermRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        String nextRoleName = normalizeRequired(request.roleName(), "Role name is required");
        String nextRoleDescription = normalizeOptional(request.roleDescription());
        String currentRoleName = role.getName();
        boolean renaming = !currentRoleName.equalsIgnoreCase(nextRoleName);

        if (renaming && role.isSystem()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System role names cannot be changed");
        }

        if (renaming) {
            ermRoleRepository.findByNameIgnoreCase(nextRoleName)
                    .filter(existing -> !existing.getId().equals(roleId))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
                    });
            propagateRoleRename(currentRoleName, nextRoleName);
            role.setName(nextRoleName);
        }

        role.setDescription(nextRoleDescription);
        role = ermRoleRepository.save(role);
        return toRoleConfigResponse(role);
    }

    private ReportingManagerConfigResponse toResponse(ErmDesignationHierarchy hierarchy) {
        ErmRole role = ermRoleRepository.findByNameIgnoreCase(hierarchy.getDesignationRoleName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designation role not found"));
        return new ReportingManagerConfigResponse(
                hierarchy.getId(),
                role.getId(),
                role.getName(),
                hierarchy.getReportsToRoleName(),
                hierarchy.getSortOrder(),
                hierarchy.isActive()
        );
    }

    private RoleConfigResponse toRoleConfigResponse(ErmRole role) {
        return new RoleConfigResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                ermUserRepository.countDistinctByRoles_Id(role.getId()),
                !role.isSystem()
        );
    }

    private void propagateRoleRename(String currentRoleName, String nextRoleName) {
        List<ErmDesignationHierarchy> hierarchyRows = new ArrayList<>(
                designationHierarchyRepository.findAllByDesignationRoleNameIgnoreCaseOrReportsToRoleNameIgnoreCase(
                        currentRoleName,
                        currentRoleName
                )
        );
        hierarchyRows.forEach(row -> {
            if (row.getDesignationRoleName().equalsIgnoreCase(currentRoleName)) {
                row.setDesignationRoleName(nextRoleName);
            }
            if (row.getReportsToRoleName().equalsIgnoreCase(currentRoleName)) {
                row.setReportsToRoleName(nextRoleName);
            }
        });
        if (!hierarchyRows.isEmpty()) {
            designationHierarchyRepository.saveAll(hierarchyRows);
        }

        List<ErmUser> users = new ArrayList<>(ermUserRepository.findAllByReportingManagerRoleNameIgnoreCase(currentRoleName));
        users.forEach(user -> user.setReportingManagerRoleName(nextRoleName));
        if (!users.isEmpty()) {
            ermUserRepository.saveAll(users);
        }
    }

    private void ensureDistinctRoles(String designationRoleName, String reportsToRoleName) {
        if (designationRoleName.equalsIgnoreCase(reportsToRoleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A role cannot report to itself");
        }
    }

    private void validateRoleExists(String roleName, String message) {
        ermRoleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
