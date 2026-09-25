-- =============================================================================
-- V45: Employee ID auto-generation + GitHub issue sync for support tickets.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Employee ID on ERM_USERS (auto-generated at on-boarding approval time).
-- ---------------------------------------------------------------------------
ALTER TABLE ERM_USERS
    ADD COLUMN EMPLOYEE_ID VARCHAR(50) NULL UNIQUE AFTER USERNAME;

-- ---------------------------------------------------------------------------
-- 2. Per-designation-code sequence counter used to build Employee IDs like
--    EMP-SE-01, EMP-SE-02, ... in a collision-safe, transactional manner.
-- ---------------------------------------------------------------------------
CREATE TABLE ERM_EMPLOYEE_ID_SEQUENCES (
                                           DESIGNATION_CODE VARCHAR(20) NOT NULL PRIMARY KEY,
                                           LAST_SEQUENCE INT NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------------
-- 3. GitHub issue linkage columns on ERM_SUPPORT_TICKETS.
-- ---------------------------------------------------------------------------
ALTER TABLE ERM_SUPPORT_TICKETS
    ADD COLUMN GITHUB_ISSUE_NUMBER INT NULL,
    ADD COLUMN GITHUB_ISSUE_URL VARCHAR(500) NULL,
    ADD COLUMN GITHUB_ISSUE_NODE_ID VARCHAR(100) NULL,
    ADD COLUMN GITHUB_PROJECT_ITEM_ID VARCHAR(100) NULL;
