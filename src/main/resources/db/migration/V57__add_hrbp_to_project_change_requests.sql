-- Keep this migration retry-safe in case the application restarts after a partial DDL run.
-- The HRBP user is validated by ProjectChangeRequestService when a request is submitted.
ALTER TABLE ERM_PROJECT_CHANGE_REQUESTS
    ADD COLUMN IF NOT EXISTS ASSOCIATED_HR_USER_ID BIGINT NULL;
