package com.org.erm.repository;

import com.org.erm.model.ErmOnboardingRequest;
import com.org.erm.model.OnboardingInterviewStage;
import com.org.erm.model.OnboardingWorkflowStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ErmOnboardingRequestRepository extends JpaRepository<ErmOnboardingRequest, Long> {

    List<ErmOnboardingRequest> findAllByInterviewStageOrderByCreatedAtDesc(OnboardingInterviewStage interviewStage);

    List<ErmOnboardingRequest> findAllByWorkflowStageOrderByCreatedAtDesc(OnboardingWorkflowStage workflowStage);

    List<ErmOnboardingRequest> findAllByOrderByCreatedAtDesc();

    Page<ErmOnboardingRequest> findAllByWorkflowStageOrderByCreatedAtDesc(OnboardingWorkflowStage workflowStage, Pageable pageable);

    Page<ErmOnboardingRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByWorkflowStageIn(Collection<OnboardingWorkflowStage> workflowStages);

    List<ErmOnboardingRequest> findAllByWorkflowStageInOrderByCreatedAtDesc(Collection<OnboardingWorkflowStage> workflowStages);

    List<ErmOnboardingRequest> findAllByWorkflowStageOrWorkflowStageInOrderByCreatedAtDesc(OnboardingWorkflowStage workflowStage, Collection<OnboardingWorkflowStage> workflowStages);
}
