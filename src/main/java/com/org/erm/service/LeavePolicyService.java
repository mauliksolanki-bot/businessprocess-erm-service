package com.org.erm.service;

import com.org.erm.dto.LeavePolicyCreateRequest;
import com.org.erm.dto.LeavePolicyResponse;
import com.org.erm.dto.LeavePolicyUpdateRequest;
import com.org.erm.model.ErmLeavePolicy;
import com.org.erm.repository.ErmLeavePolicyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class LeavePolicyService {

    private final ErmLeavePolicyRepository leavePolicyRepository;

    public LeavePolicyService(ErmLeavePolicyRepository leavePolicyRepository) {
        this.leavePolicyRepository = leavePolicyRepository;
    }

    @Transactional(readOnly = true)
    public List<LeavePolicyResponse> listPolicies() {
        return leavePolicyRepository.findAll().stream()
                .sorted(Comparator.comparing(policy -> policy.getLeaveCategory().ordinal()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LeavePolicyResponse createPolicy(LeavePolicyCreateRequest request) {
        if (leavePolicyRepository.existsByLeaveCategory(request.leaveCategory())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave policy already exists for category");
        }

        ErmLeavePolicy policy = new ErmLeavePolicy();
        policy.setLeaveCategory(request.leaveCategory());
        policy.setDisplayName(normalizeRequired(request.displayName(), "Display name is required"));
        policy.setMaxDaysPerYear(request.maxDaysPerYear());
        policy.setEnabled(request.enabled());
        return toResponse(leavePolicyRepository.save(policy));
    }

    @Transactional
    public LeavePolicyResponse updatePolicy(Long id, LeavePolicyUpdateRequest request) {
        ErmLeavePolicy policy = leavePolicyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave policy not found"));
        policy.setDisplayName(normalizeRequired(request.displayName(), "Display name is required"));
        policy.setMaxDaysPerYear(request.maxDaysPerYear());
        policy.setEnabled(request.enabled());
        return toResponse(leavePolicyRepository.save(policy));
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private LeavePolicyResponse toResponse(ErmLeavePolicy policy) {
        return new LeavePolicyResponse(
                policy.getId(),
                policy.getLeaveCategory().getLabel(),
                policy.getDisplayName(),
                policy.getMaxDaysPerYear(),
                policy.isEnabled(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
