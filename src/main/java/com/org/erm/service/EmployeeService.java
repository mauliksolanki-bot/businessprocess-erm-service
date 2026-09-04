package com.org.erm.service;

import com.org.erm.dto.response.EmployeeDirectReportResponse;
import com.org.erm.dto.response.EmployeeResponse;
import com.org.erm.dto.request.EmployeeUpdateRequest;
import com.org.erm.dto.response.PagedResponse;
import com.org.erm.dto.response.OnboardingManagerOptionResponse;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private final ErmUserRepository ermUserRepository;

    public EmployeeService(ErmUserRepository ermUserRepository) {
        this.ermUserRepository = ermUserRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> searchEmployees(String employeeName, String roleName, String department, String status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        var pageResult = ermUserRepository.searchEmployeesPage(
                PageRequest.of(safePage, safeSize),
                normalize(employeeName),
                normalize(roleName),
                normalize(department),
                normalize(status)
        );
        return PagedResponse.from(pageResult.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long employeeId) {
        return ermUserRepository.findById(employeeId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @Transactional(readOnly = true)
    public List<EmployeeDirectReportResponse> getDirectReports(Long employeeId) {
        if (!ermUserRepository.existsById(employeeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        return ermUserRepository.findAllByReportingManagerUserIdOrderByFullNameAsc(employeeId).stream()
                .map(this::toDirectReportResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnboardingManagerOptionResponse> getReplacementOptions(Long employeeId) {
        ErmUser employee = ermUserRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        String designationRoleName = resolveDesignation(employee);
        return ermUserRepository.findActiveUsersByRoleName(designationRoleName).stream()
                .filter(user -> !user.getId().equals(employeeId))
                .map(user -> new OnboardingManagerOptionResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getEmail()
                ))
                .toList();
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long employeeId, EmployeeUpdateRequest request) {
        ErmUser user = ermUserRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        user.setFullName(normalizeRequired(request.fullName(), "Full name is required"));
        String normalizedEmail = normalizeRequired(request.email(), "Email is required").toLowerCase(Locale.ROOT);
        if (ermUserRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, employeeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email address is already assigned to another employee");
        }
        user.setEmail(normalizedEmail);
        user.setDepartment(normalizeRequired(request.department(), "Department is required"));
        String employmentStatus = normalizeEmploymentStatus(request.employmentStatus());
        user.setEmploymentStatus(employmentStatus);
        user.setActive(!"inactive".equalsIgnoreCase(employmentStatus));

        return toResponse(ermUserRepository.save(user));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeEmploymentStatus(String value) {
        String normalized = normalizeRequired(value, "Employment status is required");
        if ("active".equalsIgnoreCase(normalized)) {
            return "Active";
        }
        if ("inactive".equalsIgnoreCase(normalized)) {
            return "Inactive";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employment status must be Active or Inactive");
    }

    private EmployeeResponse toResponse(ErmUser user) {
        List<String> roles = user.getRoles().stream()
                .map(ErmRole::getName)
                .sorted(Comparator.naturalOrder())
                .toList();

        ErmUser manager = user.getReportingManagerUserId() == null ? null : ermUserRepository.findById(user.getReportingManagerUserId()).orElse(null);

        return new EmployeeResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                roles,
                user.getPrimaryRoleId(),
                resolveDesignation(user),
                user.getDepartment(),
                user.getEmploymentStatus(),
                user.getReportingManagerUserId(),
                manager == null ? null : manager.getUsername(),
                manager == null ? null : manager.getFullName(),
                user.getReportingManagerRoleName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private EmployeeDirectReportResponse toDirectReportResponse(ErmUser user) {
        return new EmployeeDirectReportResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                resolveDesignation(user),
                user.getEmploymentStatus()
        );
    }

    private String resolveDesignation(ErmUser user) {
        if (user.getPrimaryRoleId() != null) {
            return user.getRoles().stream()
                    .filter(role -> role.getId().equals(user.getPrimaryRoleId()))
                    .map(ErmRole::getName)
                    .findFirst()
                    .orElseGet(() -> user.getRoles().stream()
                            .map(ErmRole::getName)
                            .sorted(Comparator.naturalOrder())
                            .findFirst()
                            .orElse("Employee"));
        }
        return user.getRoles().stream()
                .map(ErmRole::getName)
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElse("Employee");
    }
}
