package com.org.erm.service;

import com.org.erm.dto.request.BankDetailsUpsertRequest;
import com.org.erm.dto.request.UserProfileUpdateRequest;
import com.org.erm.dto.response.BankDetailsResponse;
import com.org.erm.dto.response.SelfProjectAssignmentResponse;
import com.org.erm.dto.response.UserProfileResponse;
import com.org.erm.dto.response.UserMentionOptionResponse;
import com.org.erm.model.ErmRole;
import com.org.erm.model.ErmUser;
import com.org.erm.model.ErmUserBankDetails;
import com.org.erm.model.ProjectAllocationStatus;
import com.org.erm.repository.ErmProjectAllocationRepository;
import com.org.erm.repository.ErmRoleRepository;
import com.org.erm.repository.ErmUserBankDetailsRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final String BANK_DETAILS_EDIT_WINDOW_MESSAGE =
            "You can't edit the account details right now. Please try between 1st - 5th of current month.";

    private final ErmUserRepository ermUserRepository;
    private final ErmRoleRepository ermRoleRepository;
    private final ErmProjectAllocationRepository ermProjectAllocationRepository;
    private final ErmUserBankDetailsRepository ermUserBankDetailsRepository;

    public UserService(ErmUserRepository ermUserRepository,
                       ErmRoleRepository ermRoleRepository,
                       ErmProjectAllocationRepository ermProjectAllocationRepository,
                       ErmUserBankDetailsRepository ermUserBankDetailsRepository) {
        this.ermUserRepository = ermUserRepository;
        this.ermRoleRepository = ermRoleRepository;
        this.ermProjectAllocationRepository = ermProjectAllocationRepository;
        this.ermUserBankDetailsRepository = ermUserBankDetailsRepository;
    }

    public UserProfileResponse getCurrentUserProfile(String username) {
        ErmUser ermUser = findUserByUsername(username);

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
        List<SelfProjectAssignmentResponse> currentProjects = ermProjectAllocationRepository
                .findAllByEmployeeUserIdAndStatusOrderByUpdatedAtDesc(ermUser.getId(), ProjectAllocationStatus.ACTIVE)
                .stream()
                .map(item -> new SelfProjectAssignmentResponse(
                        item.getId(),
                        item.getAllocationCode(),
                        item.getProjectRequestId(),
                        item.getProjectName(),
                        item.getProjectCode(),
                        item.getAllocationType().getLabel(),
                        item.getAllocationPercent(),
                        item.getStartDate(),
                        item.getEndDate(),
                        item.getStatus().getLabel(),
                        item.getUpdatedAt()
                ))
                .toList();

        boolean editWindowOpen = isBankDetailsEditWindowOpen();

        return new UserProfileResponse(
                ermUser.getId(),
                ermUser.getUsername(),
                ermUser.getEmail(),
                ermUser.getFullName(),
                designation,
                reportingManagerFullName,
                ermUser.getReportingManagerRoleName(),
                roleNames,
                currentProjects,
                ermUser.getPersonalEmailAddress(),
                ermUser.getPhoneNumber(),
                ermUser.getEducationQualification(),
                editWindowOpen,
                editWindowOpen ? null : BANK_DETAILS_EDIT_WINDOW_MESSAGE
        );
    }

    @Transactional
    public UserProfileResponse updateCurrentUserProfile(String username, UserProfileUpdateRequest request) {
        ErmUser ermUser = findUserByUsername(username);
        ermUser.setPersonalEmailAddress(normalizeOptional(request.personalEmailAddress()));
        ermUser.setPhoneNumber(normalizeOptional(request.phoneNumber()));
        ermUser.setEducationQualification(normalizeOptional(request.educationQualification()));
        ermUserRepository.save(ermUser);
        return getCurrentUserProfile(username);
    }

    public BankDetailsResponse getCurrentUserBankDetails(String username) {
        ErmUser ermUser = findUserByUsername(username);
        boolean editWindowOpen = isBankDetailsEditWindowOpen();
        return ermUserBankDetailsRepository.findByUserId(ermUser.getId())
                .map(details -> toBankDetailsResponse(details, editWindowOpen))
                .orElse(null);
    }

    @Transactional
    public BankDetailsResponse saveCurrentUserBankDetails(String username, BankDetailsUpsertRequest request) {
        if (!isBankDetailsEditWindowOpen()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BANK_DETAILS_EDIT_WINDOW_MESSAGE);
        }
        if (!request.accountNumber().equals(request.confirmAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account number and confirm account number must match");
        }

        ErmUser ermUser = findUserByUsername(username);
        ErmUserBankDetails bankDetails = ermUserBankDetailsRepository.findByUserId(ermUser.getId())
                .orElseGet(() -> {
                    ErmUserBankDetails created = new ErmUserBankDetails();
                    created.setUserId(ermUser.getId());
                    return created;
                });

        bankDetails.setAccountHolderName(request.accountHolderName().trim());
        bankDetails.setBankName(request.bankName().trim());
        bankDetails.setAccountNumber(request.accountNumber().trim());
        bankDetails.setIfscCode(request.ifscCode().trim().toUpperCase(Locale.ROOT));
        bankDetails.setBranchName(request.branchName().trim());
        bankDetails.setAccountType(request.accountType().trim().toUpperCase(Locale.ROOT));

        ErmUserBankDetails saved = ermUserBankDetailsRepository.save(bankDetails);
        return toBankDetailsResponse(saved, true);
    }

    private BankDetailsResponse toBankDetailsResponse(ErmUserBankDetails details, boolean editWindowOpen) {
        return new BankDetailsResponse(
                details.getId(),
                details.getAccountHolderName(),
                details.getBankName(),
                maskAccountNumber(details.getAccountNumber()),
                details.getIfscCode(),
                details.getBranchName(),
                details.getAccountType(),
                details.getUpdatedAt(),
                editWindowOpen,
                editWindowOpen ? null : BANK_DETAILS_EDIT_WINDOW_MESSAGE
        );
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        String lastFour = accountNumber.substring(accountNumber.length() - 4);
        return "*".repeat(accountNumber.length() - 4) + lastFour;
    }

    private boolean isBankDetailsEditWindowOpen() {
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        return dayOfMonth >= 1 && dayOfMonth <= 5;
    }

    private ErmUser findUserByUsername(String username) {
        return ermUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

