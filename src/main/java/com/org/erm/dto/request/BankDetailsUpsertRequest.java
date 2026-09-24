package com.org.erm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankDetailsUpsertRequest(
        @NotBlank(message = "Account holder name is required")
        @Size(max = 150, message = "Account holder name must not exceed 150 characters")
        String accountHolderName,

        @NotBlank(message = "Bank name is required")
        @Size(max = 150, message = "Bank name must not exceed 150 characters")
        String bankName,

        @NotBlank(message = "Account number is required")
        @Pattern(regexp = "^[0-9]{6,20}$", message = "Account number must be 6-20 digits")
        String accountNumber,

        @NotBlank(message = "Confirm account number is required")
        String confirmAccountNumber,

        @NotBlank(message = "IFSC code is required")
        @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "IFSC code format is invalid")
        String ifscCode,

        @NotBlank(message = "Branch name is required")
        @Size(max = 150, message = "Branch name must not exceed 150 characters")
        String branchName,

        @NotBlank(message = "Account type is required")
        @Pattern(regexp = "(?i)SAVINGS|CURRENT", message = "Account type must be SAVINGS or CURRENT")
        String accountType
) {
}
