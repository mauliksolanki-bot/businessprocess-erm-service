package com.org.erm.service;

import com.org.erm.dto.response.UserProfileResponse;
import com.org.erm.dto.response.UserMentionOptionResponse;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.repository.ErmRoleRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.PageRequest;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final ErmUserRepository ermUserRepository;
    private final ErmRoleRepository ermRoleRepository;

    public UserService(ErmUserRepository ermUserRepository, ErmRoleRepository ermRoleRepository) {
        this.ermUserRepository = ermUserRepository;
        this.ermRoleRepository = ermRoleRepository;
    }

    public UserProfileResponse getCurrentUserProfile(String username) {
        ErmUser ermUser = ermUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<String> roleNames = ermUser.getRoles().stream()
                .map(ErmRole::getName)
                .sorted(Comparator.naturalOrder())
                .toList();

        String designation = ermUser.getPrimaryRoleId() == null
                ? roleNames.stream().findFirst().orElse("Employee")
                : ermRoleRepository.findById(ermUser.getPrimaryRoleId())
                .map(ErmRole::getName)
                .orElseGet(() -> roleNames.stream().findFirst().orElse("Employee"));
        String reportingManagerFullName = null;
        if (ermUser.getReportingManagerUserId() != null) {
            reportingManagerFullName = ermUserRepository.findById(ermUser.getReportingManagerUserId())
                    .map(manager -> manager.getFullName() == null || manager.getFullName().isBlank() ? manager.getUsername() : manager.getFullName().trim())
                    .orElse(null);
        }

        return new UserProfileResponse(
                ermUser.getId(),
                ermUser.getUsername(),
                ermUser.getEmail(),
                ermUser.getFullName(),
                designation,
                reportingManagerFullName,
                ermUser.getReportingManagerRoleName(),
                roleNames
        );
    }

    public List<UserMentionOptionResponse> searchMentionableUsers(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return ermUserRepository.findAllByActiveTrueAndEmploymentStatusIgnoreCaseOrderByFullNameAsc("active")
                    .stream()
                    .limit(12)
                    .map(user -> new UserMentionOptionResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName().trim()
                    ))
                    .collect(Collectors.toList());
        }

        List<UserMentionOptionResponse> matches = ermUserRepository.searchMentionableUsers(normalizedQuery, PageRequest.of(0, 12)).stream()
                .map(user -> new UserMentionOptionResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName().trim()
                ))
                .collect(Collectors.toList());

        if (!matches.isEmpty()) {
            return matches;
        }

        return ermUserRepository.findAllByActiveTrueAndEmploymentStatusIgnoreCaseOrderByFullNameAsc("active")
                .stream()
                .limit(12)
                .map(user -> new UserMentionOptionResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName().trim()
                ))
                .collect(Collectors.toList());
    }
}
