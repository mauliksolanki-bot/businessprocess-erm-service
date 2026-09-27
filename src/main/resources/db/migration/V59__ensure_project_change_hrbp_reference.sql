-- Reconcile databases where V57/V58 were recorded but did not leave the HRBP
-- reference in place. The response resolves display name and role from ERM_USERS.
ALTER TABLE ERM_PROJECT_CHANGE_REQUESTS
    ADD COLUMN IF NOT EXISTS ASSOCIATED_HR_USER_ID BIGINT NULL;
