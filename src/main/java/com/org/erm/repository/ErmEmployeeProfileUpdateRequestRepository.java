package com.org.erm.repository;

import com.org.erm.model.ErmEmployeeProfileUpdateRequest;
import com.org.erm.model.OnboardingWorkflowStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ErmEmployeeProfileUpdateRequestRepository extends JpaRepository<ErmEmployeeProfileUpdateRequest, Long> {
    List<ErmEmployeeProfileUpdateRequest> findAllByOrderByCreatedAtDesc();
    List<ErmEmployeeProfileUpdateRequest> findAllByWorkflowStageOrderByCreatedAtDesc(OnboardingWorkflowStage workflowStage);
    boolean existsByEmployeeUserIdAndWorkflowStageNotIn(Long employeeUserId, Collection<OnboardingWorkflowStage> terminalStages);
    long countByWorkflowStageNotIn(Collection<OnboardingWorkflowStage> terminalStages);
    Page<ErmEmployeeProfileUpdateRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ErmEmployeeProfileUpdateRequest> findAllByWorkflowStageOrderByCreatedAtDesc(OnboardingWorkflowStage workflowStage, Pageable pageable);
}
