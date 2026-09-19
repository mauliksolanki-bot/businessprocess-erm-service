package com.org.erm.repository;

import com.org.erm.model.ErmAttendanceLeaveReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmAttendanceLeaveReconciliationRepository extends JpaRepository<ErmAttendanceLeaveReconciliation, Long> {
    Optional<ErmAttendanceLeaveReconciliation> findByLeaveRequestIdAndTimesheetId(Long leaveRequestId, Long timesheetId);

    List<ErmAttendanceLeaveReconciliation> findAllByLeaveRequestIdAndRestoredAtIsNullOrderByWeekStartDateAsc(Long leaveRequestId);
}
