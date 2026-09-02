package com.org.erm.repository;

import com.org.erm.model.ErmProjectAllocation;
import com.org.erm.model.ProjectAllocationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Collection;

public interface ErmProjectAllocationRepository extends JpaRepository<ErmProjectAllocation, Long> {

    @Query("""
            SELECT a
            FROM ErmProjectAllocation a
            WHERE (:createdByUsername IS NULL OR LOWER(a.createdByUsername) = LOWER(:createdByUsername))
              AND (:status IS NULL OR a.status = :status)
              AND (:query IS NULL OR LOWER(a.projectName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(a.projectCode) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(a.employeeName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(a.allocationCode) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY a.updatedAt DESC
            """)
    Page<ErmProjectAllocation> search(
            @Param("createdByUsername") String createdByUsername,
            @Param("status") ProjectAllocationStatus status,
            @Param("query") String query,
            Pageable pageable
    );

    List<ErmProjectAllocation> findAllByStatusOrderByCreatedAtDesc(ProjectAllocationStatus status);
    List<ErmProjectAllocation> findAllByEmployeeUserIdInAndStatus(Collection<Long> employeeUserIds, ProjectAllocationStatus status);

    @Query("""
            SELECT COALESCE(SUM(a.allocationPercent), 0)
            FROM ErmProjectAllocation a
            WHERE a.employeeUserId = :employeeUserId
              AND a.status = :status
              AND (:excludeId IS NULL OR a.id <> :excludeId)
              AND a.startDate <= :endDate
              AND a.endDate >= :startDate
            """)
    BigDecimal sumAllocationPercentForOverlap(
            @Param("employeeUserId") Long employeeUserId,
            @Param("status") ProjectAllocationStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );
}
