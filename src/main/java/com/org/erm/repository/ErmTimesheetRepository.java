package com.org.erm.repository;

import com.org.erm.model.ErmTimesheet;
import com.org.erm.model.TimesheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmTimesheetRepository extends JpaRepository<ErmTimesheet, Long> {
    List<ErmTimesheet> findAllByEmployeeUserIdOrderByWeekStartDateDesc(Long employeeUserId);

    List<ErmTimesheet> findAllByReportingManagerUserIdAndStatusOrderByWeekStartDateDesc(Long reportingManagerUserId, TimesheetStatus status);
}
