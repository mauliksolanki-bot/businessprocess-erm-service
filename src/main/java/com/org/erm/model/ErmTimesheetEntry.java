package com.org.erm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ERM_TIMESHEET_ENTRIES")
public class ErmTimesheetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TIMESHEET_ID", nullable = false)
    private Long timesheetId;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "WORK_TYPE", nullable = false, length = 30)
    private TimesheetWorkType workType;

    @Column(name = "PROJECT_ALLOCATION_ID")
    private Long projectAllocationId;

    @Column(name = "PROJECT_NAME", length = 150)
    private String projectName;

    @Column(name = "PROJECT_CODE", length = 30)
    private String projectCode;

    @Column(name = "TASK_NAME", nullable = false, length = 255)
    private String taskName;

    @Column(name = "MONDAY_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal mondayHours;

    @Column(name = "TUESDAY_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal tuesdayHours;

    @Column(name = "WEDNESDAY_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal wednesdayHours;

    @Column(name = "THURSDAY_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal thursdayHours;

    @Column(name = "FRIDAY_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal fridayHours;

    @Column(name = "SATURDAY_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal saturdayHours;

    @Column(name = "SUNDAY_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal sundayHours;

    @Column(name = "TOTAL_HOURS", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalHours;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getTimesheetId() {
        return timesheetId;
    }

    public void setTimesheetId(Long timesheetId) {
        this.timesheetId = timesheetId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public TimesheetWorkType getWorkType() {
        return workType;
    }

    public void setWorkType(TimesheetWorkType workType) {
        this.workType = workType;
    }

    public Long getProjectAllocationId() {
        return projectAllocationId;
    }

    public void setProjectAllocationId(Long projectAllocationId) {
        this.projectAllocationId = projectAllocationId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public BigDecimal getMondayHours() {
        return mondayHours;
    }

    public void setMondayHours(BigDecimal mondayHours) {
        this.mondayHours = mondayHours;
    }

    public BigDecimal getTuesdayHours() {
        return tuesdayHours;
    }

    public void setTuesdayHours(BigDecimal tuesdayHours) {
        this.tuesdayHours = tuesdayHours;
    }

    public BigDecimal getWednesdayHours() {
        return wednesdayHours;
    }

    public void setWednesdayHours(BigDecimal wednesdayHours) {
        this.wednesdayHours = wednesdayHours;
    }

    public BigDecimal getThursdayHours() {
        return thursdayHours;
    }

    public void setThursdayHours(BigDecimal thursdayHours) {
        this.thursdayHours = thursdayHours;
    }

    public BigDecimal getFridayHours() {
        return fridayHours;
    }

    public void setFridayHours(BigDecimal fridayHours) {
        this.fridayHours = fridayHours;
    }

    public BigDecimal getSaturdayHours() {
        return saturdayHours;
    }

    public void setSaturdayHours(BigDecimal saturdayHours) {
        this.saturdayHours = saturdayHours;
    }

    public BigDecimal getSundayHours() {
        return sundayHours;
    }

    public void setSundayHours(BigDecimal sundayHours) {
        this.sundayHours = sundayHours;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
