-- =============================================================================
-- V39: Attendance and weekly timesheet foundation
-- Creates weekly timesheets, daily entries, and associated workflow columns.
-- =============================================================================

CREATE TABLE ERM_ATTENDANCE_TIMESHEETS (
                                           ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           EMPLOYEE_USER_ID BIGINT NOT NULL,
                                           EMPLOYEE_USERNAME VARCHAR(100) NOT NULL,
                                           EMPLOYEE_FULL_NAME VARCHAR(150) NOT NULL,
                                           WEEK_START_DATE DATE NOT NULL,
                                           WEEK_END_DATE DATE NOT NULL,
                                           APPROVER_MANAGER_USER_ID BIGINT NULL,
                                           APPROVER_MANAGER_USERNAME VARCHAR(100) NULL,
                                           APPROVER_MANAGER_FULL_NAME VARCHAR(150) NULL,
                                           TIMESHEET_STATUS VARCHAR(30) NOT NULL,
                                           APPROVAL_REQUIRED TINYINT(1) NOT NULL DEFAULT 0,
                                           SUBMITTED_AT TIMESTAMP NULL,
                                           APPROVED_AT TIMESTAMP NULL,
                                           REJECTED_AT TIMESTAMP NULL,
                                           APPROVER_COMMENT VARCHAR(500) NULL,
                                           CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           UPDATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                           UNIQUE KEY UK_ERM_ATTENDANCE_TIMESHEETS_EMPLOYEE_WEEK (EMPLOYEE_USER_ID, WEEK_START_DATE)
);

CREATE TABLE ERM_ATTENDANCE_TIMESHEET_DAYS (
                                               ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               TIMESHEET_ID BIGINT NOT NULL,
                                               WORK_DATE DATE NOT NULL,
                                               BILLABLE_HOURS DECIMAL(4,2) NOT NULL DEFAULT 0.00,
                                               NON_BILLABLE_HOURS DECIMAL(4,2) NOT NULL DEFAULT 0.00,
                                               BILLABLE_PROJECT_ALLOCATION_ID BIGINT NULL,
                                               BILLABLE_PROJECT_REQUEST_ID BIGINT NULL,
                                               BILLABLE_PROJECT_NAME VARCHAR(150) NULL,
                                               BILLABLE_PROJECT_CODE VARCHAR(30) NULL,
                                               NON_BILLABLE_PROJECT_ALLOCATION_ID BIGINT NULL,
                                               NON_BILLABLE_PROJECT_REQUEST_ID BIGINT NULL,
                                               NON_BILLABLE_PROJECT_NAME VARCHAR(150) NULL,
                                               NON_BILLABLE_PROJECT_CODE VARCHAR(30) NULL,
                                               CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               UPDATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                               CONSTRAINT FK_ERM_ATTENDANCE_TIMESHEET_DAYS_TIMESHEET FOREIGN KEY (TIMESHEET_ID) REFERENCES ERM_ATTENDANCE_TIMESHEETS (ID) ON DELETE CASCADE,
                                               UNIQUE KEY UK_ERM_ATTENDANCE_TIMESHEET_DAYS (TIMESHEET_ID, WORK_DATE)
);
