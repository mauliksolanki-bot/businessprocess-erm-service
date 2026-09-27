-- V57 may already be recorded as applied on databases where its first attempt
-- partially changed the schema. Ensure every mapped column exists independently.
ALTER TABLE ERM_PROJECT_CHANGE_REQUESTS
    ADD COLUMN IF NOT EXISTS ASSOCIATED_HR_USER_ID BIGINT NULL,
    ADD COLUMN IF NOT EXISTS ASSOCIATED_HR_NAME VARCHAR(150) NULL,
    ADD COLUMN IF NOT EXISTS ASSOCIATED_HR_ROLE_NAME VARCHAR(100) NULL;
