package com.org.erm.repository;

import com.org.erm.model.ErmUserBankDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ErmUserBankDetailsRepository extends JpaRepository<ErmUserBankDetails, Long> {

    Optional<ErmUserBankDetails> findByUserId(Long userId);
}
