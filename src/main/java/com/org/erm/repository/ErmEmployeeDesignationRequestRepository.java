package com.org.erm.repository;

import com.org.erm.model.ErmEmployeeDesignationRequest;
import com.org.erm.model.OnboardingWorkflowStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmEmployeeDesignationRequestRepository extends JpaRepository<ErmEmployeeDesignationRequest, Long> {

    List<ErmEmployeeDesignationRequest> findAllByWorkflowStageOrderByCreatedAtDesc(OnboardingWorkflowStage workflowStage);

    List<ErmEmployeeDesignationRequest> findAllByOrderByCreatedAtDesc();
}
