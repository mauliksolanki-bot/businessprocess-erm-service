package com.org.erm.repository;

import com.org.erm.model.ErmAttendanceTimesheetDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmAttendanceTimesheetDayRepository extends JpaRepository<ErmAttendanceTimesheetDay, Long> {
    List<ErmAttendanceTimesheetDay> findAllByTimesheetIdOrderByWorkDateAsc(Long timesheetId);

    void deleteAllByTimesheetId(Long timesheetId);
}
