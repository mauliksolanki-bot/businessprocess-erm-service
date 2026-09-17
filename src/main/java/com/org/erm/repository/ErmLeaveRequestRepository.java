package com.org.erm.repository;

import com.org.erm.model.ErmLeaveRequest;
import com.org.erm.model.LeaveCategory;
import com.org.erm.model.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ErmLeaveRequestRepository extends JpaRepository<ErmLeaveRequest, Long> {
    List<ErmLeaveRequest> findAllByEmployeeUserIdOrderByCreatedAtDesc(Long employeeUserId);
    List<ErmLeaveRequest> findAllByApproverManagerUserIdOrderByCreatedAtDesc(Long approverManagerUserId);
    List<ErmLeaveRequest> findAllByEmployeeUserIdInAndRequestStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
            Collection<Long> employeeUserIds,
            LeaveRequestStatus requestStatus,
            LocalDate monthEnd,
            LocalDate monthStart
    );

    List<ErmLeaveRequest> findAllByEmployeeUserIdAndRequestStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
            Long employeeUserId,
            LeaveRequestStatus requestStatus,
            LocalDate monthEnd,
            LocalDate monthStart
    );

    @Query("""
            SELECT COALESCE(SUM(r.requestedDays), 0)
            FROM ErmLeaveRequest r
            WHERE r.employeeUserId = :employeeUserId
              AND r.leaveCategory = :leaveCategory
              AND r.requestStatus IN :statuses
              AND r.startDate >= :yearStart
              AND r.startDate <= :yearEnd
            """)
    Long sumRequestedDaysByYear(@Param("employeeUserId") Long employeeUserId,
                                @Param("leaveCategory") LeaveCategory leaveCategory,
                                @Param("statuses") Collection<LeaveRequestStatus> statuses,
                                @Param("yearStart") LocalDate yearStart,
                                @Param("yearEnd") LocalDate yearEnd);
}
