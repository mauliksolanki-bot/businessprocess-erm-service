-- Rework onboarding approval chain (remove mandatory CHRO step, add optional additional-approval step)
-- and add self-service profile fields + bank details.

-- 1. Rename onboarding approval columns: 2nd approval is now Admin (was CHRO); final optional approval is
--    now a configurable "additional approver" (was hard-coded Super Admin).
ALTER TABLE ERM_ONBOARDING_REQUESTS
    CHANGE COLUMN CHRO_COMMENT ADMIN_COMMENT VARCHAR(500) NULL,
    CHANGE COLUMN CHRO_ACTION_BY ADMIN_ACTION_BY VARCHAR(100) NULL,
    CHANGE COLUMN CHRO_ACTION_AT ADMIN_ACTION_AT TIMESTAMP NULL,
    CHANGE COLUMN SUPER_ADMIN_COMMENT ADDITIONAL_APPROVER_COMMENT VARCHAR(500) NULL,
    CHANGE COLUMN SUPER_ADMIN_ACTION_BY ADDITIONAL_APPROVER_ACTION_BY VARCHAR(100) NULL,
    CHANGE COLUMN SUPER_ADMIN_ACTION_AT ADDITIONAL_APPROVER_ACTION_AT TIMESTAMP NULL,
    ADD COLUMN ADDITIONAL_APPROVER_DESIGNATION VARCHAR(30) NULL;

-- 2. Defensive remap of any stale in-flight rows using the old workflow stage names.
UPDATE ERM_ONBOARDING_REQUESTS
SET WORKFLOW_STAGE = 'ADMIN_APPROVED'
WHERE WORKFLOW_STAGE IN ('CHRO_APPROVED', 'SUPER_ADMIN_APPROVED');

-- 3. Self-service profile fields on ERM_USERS.
ALTER TABLE ERM_USERS
    ADD COLUMN PERSONAL_EMAIL_ADDRESS VARCHAR(150) NULL,
    ADD COLUMN PHONE_NUMBER VARCHAR(25) NULL,
    ADD COLUMN EDUCATION_QUALIFICATION VARCHAR(255) NULL;

-- 4. Bank details - exactly one account per user.
CREATE TABLE ERM_USER_BANK_DETAILS (
                                       ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       USER_ID BIGINT NOT NULL UNIQUE,
                                       ACCOUNT_HOLDER_NAME VARCHAR(150) NOT NULL,
                                       BANK_NAME VARCHAR(150) NOT NULL,
                                       ACCOUNT_NUMBER VARCHAR(30) NOT NULL,
                                       IFSC_CODE VARCHAR(15) NOT NULL,
                                       BRANCH_NAME VARCHAR(150) NOT NULL,
                                       ACCOUNT_TYPE VARCHAR(20) NOT NULL,
                                       CREATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       UPDATED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       CONSTRAINT FK_ERM_USER_BANK_DETAILS_USER FOREIGN KEY (USER_ID) REFERENCES ERM_USERS (ID)
);

-- 5. "My Profile" navigation entry, visible to every role.
INSERT INTO ERM_NAV_MENUS (MENU_CODE, MENU_TITLE, MENU_PATH, MENU_ICON, SORT_ORDER)
VALUES ('my-profile', 'My Profile', '/profile', 'user-circle', 7);

INSERT INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
         JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'my-profile';
