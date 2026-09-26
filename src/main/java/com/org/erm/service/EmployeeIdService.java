package com.org.erm.service;

import com.org.erm.model.ErmEmployeeIdSequence;
import com.org.erm.model.ErmEmployeeIdRoleFormat;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.repository.ErmEmployeeIdSequenceRepository;
import com.org.erm.repository.ErmEmployeeIdRoleFormatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Generates employee identifiers from the user's primary role. */
@Service
public class EmployeeIdService {

    private final ErmEmployeeIdSequenceRepository sequenceRepository;
    private final ErmEmployeeIdRoleFormatRepository roleFormatRepository;

    public EmployeeIdService(ErmEmployeeIdSequenceRepository sequenceRepository,
                             ErmEmployeeIdRoleFormatRepository roleFormatRepository) {
        this.sequenceRepository = sequenceRepository;
        this.roleFormatRepository = roleFormatRepository;
    }

    @Transactional
    public String generateForRole(String roleName) {
        ErmEmployeeIdRoleFormat format = formatFor(roleName);
        if (!format.isSequenceEnabled()) {
            return format.getIdPrefix();
        }

        ErmEmployeeIdSequence sequence = sequenceRepository.findByIdForUpdate(format.getIdPrefix())
                .orElseThrow(() -> new IllegalStateException("Employee ID sequence is not initialized for " + format.getIdPrefix()));
        int next = sequence.getLastSequence() + 1;
        sequence.setLastSequence(next);
        sequenceRepository.save(sequence);
        return format.getIdPrefix() + "-" + String.format(Locale.ROOT, "%02d", next);
    }

    /** Refreshes an identifier only when it is missing or no longer matches the primary role. */
    @Transactional
    public void refreshForPrimaryRole(ErmUser user) {
        String primaryRoleName = user.getRoles().stream()
                .filter(role -> user.getPrimaryRoleId() != null && user.getPrimaryRoleId().equals(role.getId()))
                .map(ErmRole::getName)
                .findFirst()
                .orElseGet(() -> user.getRoles().stream()
                        .map(ErmRole::getName)
                        .filter(role -> roleFormatRepository.findByRoleNameIgnoreCase(role).isPresent())
                        .sorted((left, right) -> Integer.compare(
                                formatFor(left).getPriorityOrder(), formatFor(right).getPriorityOrder()))
                        .findFirst()
                        .orElse("Employee"));

        ErmEmployeeIdRoleFormat format = formatFor(primaryRoleName);
        String currentId = user.getEmployeeId();
        boolean alreadyMatches = format.isSequenceEnabled()
                ? currentId != null && currentId.matches(java.util.regex.Pattern.quote(format.getIdPrefix()) + "-\\d+")
                : format.getIdPrefix().equals(currentId);
        if (!alreadyMatches) {
            user.setEmployeeId(generateForRole(primaryRoleName));
        }
    }

    private ErmEmployeeIdRoleFormat formatFor(String roleName) {
        return roleFormatRepository.findByRoleNameIgnoreCase(roleName == null ? "" : roleName.trim())
                .orElseGet(() -> roleFormatRepository.findByRoleNameIgnoreCase("Employee")
                        .orElseThrow(() -> new IllegalStateException("Employee ID format for Employee is not initialized")));
    }
}
