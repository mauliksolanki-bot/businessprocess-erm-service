package com.org.erm.repository;

import com.org.erm.model.ErmSupportSlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ErmSupportSlaPolicyRepository extends JpaRepository<ErmSupportSlaPolicy, Long> {

    Optional<ErmSupportSlaPolicy> findByQueueCodeIgnoreCaseAndTicketTypeIgnoreCaseAndPriorityCodeIgnoreCaseAndActiveTrue(
            String queueCode,
            String ticketType,
            String priorityCode
    );
}
