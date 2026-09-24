package com.org.erm.dto.response;

import java.time.LocalDateTime;

public record BankDetailsResponse(
        Long id,
        String accountHolderName,
        String bankName,
        String maskedAccountNumber,
        String ifscCode,
        String branchName,
        String accountType,
        LocalDateTime updatedAt,
        boolean editWindowOpen,
        String editWindowMessage
) {
}
