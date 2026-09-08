package com.org.erm.repository;

import com.org.erm.model.ErmTimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmTimesheetEntryRepository extends JpaRepository<ErmTimesheetEntry, Long> {
    List<ErmTimesheetEntry> findAllByTimesheetIdOrderBySortOrderAsc(Long timesheetId);
}
