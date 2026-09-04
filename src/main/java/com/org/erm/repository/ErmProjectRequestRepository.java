package com.org.erm.repository;

import com.org.erm.model.ErmProjectRequest;
import com.org.erm.model.ProjectWorkflowStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ErmProjectRequestRepository extends JpaRepository<ErmProjectRequest, Long> {

    boolean existsByProjectCodeIgnoreCase(String projectCode);

    boolean existsByProjectCodeIgnoreCaseAndIdNot(String projectCode, Long id);

    boolean existsByProjectNameIgnoreCase(String projectName);

    boolean existsByProjectNameIgnoreCaseAndIdNot(String projectName, Long id);

    @Query("""
            SELECT p FROM ErmProjectRequest p
            WHERE (
                    :restrictScope = false
                    OR (:projectOwnerUserId IS NOT NULL AND p.projectOwnerUserId = :projectOwnerUserId)
                    OR (:projectManagerUserId IS NOT NULL AND p.projectManagerUserId = :projectManagerUserId)
                    OR (:projectDirectorUserId IS NOT NULL AND p.projectDirectorUserId = :projectDirectorUserId)
                    OR (:deliveryManagerUserId IS NOT NULL AND p.deliveryManagerUserId = :deliveryManagerUserId)
                  )
              AND (:workflowStage IS NULL OR p.workflowStage = :workflowStage)
              AND (:query IS NULL OR LOWER(p.projectName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.projectCode) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.clientName) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<ErmProjectRequest> search(
            @Param("restrictScope") boolean restrictScope,
            @Param("projectOwnerUserId") Long projectOwnerUserId,
            @Param("projectManagerUserId") Long projectManagerUserId,
            @Param("projectDirectorUserId") Long projectDirectorUserId,
            @Param("deliveryManagerUserId") Long deliveryManagerUserId,
            @Param("workflowStage") ProjectWorkflowStage workflowStage,
            @Param("query") String query,
            Pageable pageable
    );

    List<ErmProjectRequest> findAllByWorkflowStageInOrderByCreatedAtDesc(Collection<ProjectWorkflowStage> workflowStages);

    List<ErmProjectRequest> findAllByWorkflowStageAndCreatedByUsernameIgnoreCaseOrderByProjectNameAsc(
            ProjectWorkflowStage workflowStage,
            String createdByUsername
    );

    List<ErmProjectRequest> findAllByWorkflowStageAndProjectOwnerUserIdOrderByProjectNameAsc(
            ProjectWorkflowStage workflowStage,
            Long projectOwnerUserId
    );

    List<ErmProjectRequest> findAllByWorkflowStageAndProjectManagerUserIdOrderByProjectNameAsc(
            ProjectWorkflowStage workflowStage,
            Long projectManagerUserId
    );

    List<ErmProjectRequest> findAllByWorkflowStageOrderByProjectNameAsc(ProjectWorkflowStage workflowStage);

    @Query("""
            SELECT p
            FROM ErmProjectRequest p
            WHERE (
                p.projectOwnerUserId = :userId
                OR p.projectManagerUserId = :userId
                OR p.projectDirectorUserId = :userId
                OR p.deliveryManagerUserId = :userId
            )
            ORDER BY p.projectName ASC
            """)
    List<ErmProjectRequest> findAssociatedProjects(
            @Param("userId") Long userId
    );
}
