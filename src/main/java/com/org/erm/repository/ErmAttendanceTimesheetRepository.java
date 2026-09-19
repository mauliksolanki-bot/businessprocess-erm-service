package com.org.erm.repository;

import com.org.erm.model.AttendanceTimesheetStatus;
import com.org.erm.model.ErmAttendanceTimesheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmAttendanceTimesheetRepository extends JpaRepository<ErmAttendanceTimesheet, Long> {
    Optional<ErmAttendanceTimesheet> findByEmployeeUserIdAndWeekStartDate(Long employeeUserId, java.time.LocalDate weekStartDate);

    List<ErmAttendanceTimesheet> findAllByApproverManagerUserIdAndTimesheetStatusOrderByUpdatedAtDesc(Long approverManagerUserId,
                                                                                                      AttendanceTimesheetStatus timesheetStatus);
}
