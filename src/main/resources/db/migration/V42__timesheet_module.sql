-- =============================================================================
-- V42: Create weekly timesheet module
-- =============================================================================

CREATE TABLE ERM_TIMESHEETS (
                                ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                                TIMESHEET_CODE VARCHAR(40) NOT NULL UNIQUE,
                                EMPLOYEE_USER_ID BIGINT NOT NULL,
                                EMPLOYEE_USERNAME VARCHAR(100) NOT NULL,
                                EMPLOYEE_FULL_NAME VARCHAR(150) NOT NULL,
                                REPORTING_MANAGER_USER_ID BIGINT NULL,
                                REPORTING_MANAGER_USERNAME VARCHAR(100) NULL,
                                REPORTING_MANAGER_FULL_NAME VARCHAR(150) NULL,
                                WEEK_START_DATE DATE NOT NULL,
                                WEEK_END_DATE DATE NOT NULL,
                                STATUS VARCHAR(50) NOT NULL,
                                SUBMISSION_COMMENT VARCHAR(500) NULL,
                                MANAGER_ACTION_BY_USERNAME VARCHAR(100) NULL,
                                MANAGER_ACTION_AT TIMESTAMP NULL,
                                MANAGER_COMMENT VARCHAR(500) NULL,
                                TOTAL_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                BILLABLE_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                NON_BILLABLE_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                VERSION BIGINT NOT NULL DEFAULT 0,
                                CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                UPDATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                CONSTRAINT FK_ERM_TIMESHEETS_EMPLOYEE FOREIGN KEY (EMPLOYEE_USER_ID) REFERENCES ERM_USERS (ID),
                                CONSTRAINT FK_ERM_TIMESHEETS_MANAGER FOREIGN KEY (REPORTING_MANAGER_USER_ID) REFERENCES ERM_USERS (ID),
                                UNIQUE KEY UK_ERM_TIMESHEETS_EMPLOYEE_WEEK (EMPLOYEE_USER_ID, WEEK_START_DATE)
);

CREATE TABLE ERM_TIMESHEET_ENTRIES (
                                       ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       TIMESHEET_ID BIGINT NOT NULL,
                                       SORT_ORDER INT NOT NULL,
                                       WORK_TYPE VARCHAR(30) NOT NULL,
                                       PROJECT_ALLOCATION_ID BIGINT NULL,
                                       PROJECT_NAME VARCHAR(150) NULL,
                                       PROJECT_CODE VARCHAR(30) NULL,
                                       TASK_NAME VARCHAR(255) NOT NULL,
                                       MONDAY_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       TUESDAY_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       WEDNESDAY_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       THURSDAY_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       FRIDAY_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       SATURDAY_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       SUNDAY_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       TOTAL_HOURS DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       UPDATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       CONSTRAINT FK_ERM_TIMESHEET_ENTRIES_TIMESHEET FOREIGN KEY (TIMESHEET_ID) REFERENCES ERM_TIMESHEETS (ID),
                                       CONSTRAINT FK_ERM_TIMESHEET_ENTRIES_ALLOCATION FOREIGN KEY (PROJECT_ALLOCATION_ID) REFERENCES ERM_PROJECT_ALLOCATIONS (ID)
);
