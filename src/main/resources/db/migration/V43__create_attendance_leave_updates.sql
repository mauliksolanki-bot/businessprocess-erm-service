CREATE TABLE ERM_ATTENDANCE_LEAVE_RECONCILIATIONS (
                                                      ID BIGINT NOT NULL AUTO_INCREMENT,
                                                      LEAVE_REQUEST_ID BIGINT NOT NULL,
                                                      TIMESHEET_ID BIGINT NOT NULL,
                                                      EMPLOYEE_USER_ID BIGINT NOT NULL,
                                                      WEEK_START_DATE DATE NOT NULL,
                                                      PREVIOUS_TIMESHEET_STATUS VARCHAR(30) NOT NULL,
                                                      PREVIOUS_APPROVAL_REQUIRED BIT NOT NULL,
                                                      PREVIOUS_SUBMITTED_AT DATETIME NULL,
                                                      PREVIOUS_APPROVED_AT DATETIME NULL,
                                                      PREVIOUS_REJECTED_AT DATETIME NULL,
                                                      PREVIOUS_APPROVER_COMMENT VARCHAR(500) NULL,
                                                      RESTORED_AT DATETIME NULL,
                                                      CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                      UPDATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                      CONSTRAINT PK_ERM_ATTENDANCE_LEAVE_RECONCILIATIONS PRIMARY KEY (ID),
                                                      CONSTRAINT UK_ERM_ATTENDANCE_LEAVE_RECONCILIATIONS UNIQUE (LEAVE_REQUEST_ID, TIMESHEET_ID),
                                                      CONSTRAINT FK_ERM_ATTENDANCE_LEAVE_RECON_LEAVE FOREIGN KEY (LEAVE_REQUEST_ID) REFERENCES ERM_LEAVE_REQUESTS (ID),
                                                      CONSTRAINT FK_ERM_ATTENDANCE_LEAVE_RECON_TS FOREIGN KEY (TIMESHEET_ID) REFERENCES ERM_ATTENDANCE_TIMESHEETS (ID)
);

CREATE TABLE ERM_ATTENDANCE_LEAVE_RECONCILIATION_DAYS (
                                                          ID BIGINT NOT NULL AUTO_INCREMENT,
                                                          RECONCILIATION_ID BIGINT NOT NULL,
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
                                                          CONSTRAINT PK_ERM_ATTENDANCE_LEAVE_RECON_DAYS PRIMARY KEY (ID),
                                                          CONSTRAINT UK_ERM_ATTENDANCE_LEAVE_RECON_DAYS UNIQUE (RECONCILIATION_ID, WORK_DATE),
                                                          CONSTRAINT FK_ERM_ATTENDANCE_LEAVE_RECON_DAYS_HDR FOREIGN KEY (RECONCILIATION_ID)
                                                              REFERENCES ERM_ATTENDANCE_LEAVE_RECONCILIATIONS (ID) ON DELETE CASCADE
);
