-- =============================================================================
-- V31: Insert Director approval step into Project Tracker workflow
--      and fix V30 nav-menu grants for IT Support roles
--
-- New project workflow order:
--   PM Submitted
--     -> Delivery Manager Approved
--       -> Project Owner Approved
--         -> Director Approved   (NEW)
--           -> CTO Approved
--             -> Super Admin Approved
--
-- IMPORTANT: ALTER TABLE is split into individual statements
-- because TiDB does not support AFTER <column> when that column
-- is being added in the same multi-column ALTER TABLE statement.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Add Director action columns to ERM_PROJECT_REQUESTS (one per statement)
-- ---------------------------------------------------------------------------
ALTER TABLE ERM_PROJECT_REQUESTS ADD COLUMN DIRECTOR_ACTION_BY VARCHAR(100) NULL AFTER CTO_COMMENT;
ALTER TABLE ERM_PROJECT_REQUESTS ADD COLUMN DIRECTOR_ACTION_AT TIMESTAMP    NULL AFTER DIRECTOR_ACTION_BY;
ALTER TABLE ERM_PROJECT_REQUESTS ADD COLUMN DIRECTOR_COMMENT   VARCHAR(500) NULL AFTER DIRECTOR_ACTION_AT;

-- ---------------------------------------------------------------------------
-- 2. Fix nav-menu grants: Application Support Specialist — Leaves + Dashboard only
-- ---------------------------------------------------------------------------
DELETE rnm FROM ERM_ROLE_NAV_MENUS rnm
JOIN ERM_ROLES r ON r.ID = rnm.ROLE_ID
JOIN ERM_NAV_MENUS m ON m.ID = rnm.MENU_ID
WHERE r.ROLE_NAME = 'Application Support Specialist'
  AND m.MENU_CODE = 'attendance';

-- ---------------------------------------------------------------------------
-- 3. Fix nav-menu grants: IT Security — Leaves + Dashboard only
-- ---------------------------------------------------------------------------
DELETE rnm FROM ERM_ROLE_NAV_MENUS rnm
JOIN ERM_ROLES r ON r.ID = rnm.ROLE_ID
JOIN ERM_NAV_MENUS m ON m.ID = rnm.MENU_ID
WHERE r.ROLE_NAME = 'IT Security'
  AND m.MENU_CODE = 'attendance';

-- ---------------------------------------------------------------------------
-- 4. Fix nav-menu grants: IT Support Manager — same as Project Manager
--    (Project Manager does NOT have Employees access)
-- ---------------------------------------------------------------------------
DELETE rnm FROM ERM_ROLE_NAV_MENUS rnm
JOIN ERM_ROLES r ON r.ID = rnm.ROLE_ID
JOIN ERM_NAV_MENUS m ON m.ID = rnm.MENU_ID
WHERE r.ROLE_NAME = 'IT Support Manager'
  AND m.MENU_CODE = 'employees';

-- ---------------------------------------------------------------------------
-- 5. IT Support Manager and IT Support Lead: ensure Projects access
--    (IT Support Lead = Team Lead which has Projects)
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE = 'projects'
WHERE r.ROLE_NAME IN ('IT Support Lead', 'IT Support Manager');

-- ---------------------------------------------------------------------------
-- 6. Director: ensure Dashboard, Projects, Settings, Leaves access
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO ERM_ROLE_NAV_MENUS (ROLE_ID, MENU_ID)
SELECT r.ID, m.ID
FROM ERM_ROLES r
JOIN ERM_NAV_MENUS m ON m.MENU_CODE IN ('dashboard', 'projects', 'settings', 'leaves')
WHERE r.ROLE_NAME = 'Director';
