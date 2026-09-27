-- Ensure the mapped HRBP key exists on databases where an earlier V57 attempt
-- was recorded as applied before the column was added.
ALTER TABLE ERM_PROJECT_CHANGE_REQUESTS
    ADD COLUMN IF NOT EXISTS ASSOCIATED_HR_USER_ID BIGINT NULL;
