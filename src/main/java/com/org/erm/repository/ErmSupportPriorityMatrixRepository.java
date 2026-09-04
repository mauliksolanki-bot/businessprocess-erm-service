package com.org.erm.repository;

import com.org.erm.model.ErmSupportPriorityMatrix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ErmSupportPriorityMatrixRepository extends JpaRepository<ErmSupportPriorityMatrix, Long> {

    Optional<ErmSupportPriorityMatrix> findByImpactLevelIgnoreCaseAndUrgencyLevelIgnoreCaseAndActiveTrue(String impactLevel, String urgencyLevel);
}
