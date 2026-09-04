package com.org.erm.repository;

import com.org.erm.model.ErmProjectChangeRequest;
import com.org.erm.model.ProjectChangeWorkflowStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ErmProjectChangeRequestRepository extends JpaRepository<ErmProjectChangeRequest, Long> {

    boolean existsByProjectRequestIdAndWorkflowStageIn(Long projectRequestId, Collection<ProjectChangeWorkflowStage> workflowStages);

    List<ErmProjectChangeRequest> findAllByCreatedByUsernameIgnoreCaseOrderByCreatedAtDesc(String createdByUsername);

    List<ErmProjectChangeRequest> findAllByWorkflowStageInOrderByCreatedAtDesc(Collection<ProjectChangeWorkflowStage> workflowStages);

    List<ErmProjectChangeRequest> findAllByDeliveryManagerUserIdOrderByCreatedAtDesc(Long deliveryManagerUserId);

    List<ErmProjectChangeRequest> findAllByProjectOwnerUserIdOrderByCreatedAtDesc(Long projectOwnerUserId);

    List<ErmProjectChangeRequest> findAllByProjectDirectorUserIdOrderByCreatedAtDesc(Long projectDirectorUserId);

    List<ErmProjectChangeRequest> findAllByProjectRequestIdOrderByCreatedAtDesc(Long projectRequestId);
}
