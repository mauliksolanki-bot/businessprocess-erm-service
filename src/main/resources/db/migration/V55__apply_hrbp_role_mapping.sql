CREATE TEMPORARY TABLE ERM_HRBP_ROLE_MAPPING (
    DESIGNATION_ROLE_NAME VARCHAR(100) NOT NULL PRIMARY KEY,
    ASSOCIATED_HR_ROLE_NAME VARCHAR(100) NOT NULL
);

INSERT INTO ERM_HRBP_ROLE_MAPPING (DESIGNATION_ROLE_NAME, ASSOCIATED_HR_ROLE_NAME) VALUES
    ('Super Admin', 'Senior HR'),
    ('Admin', 'Senior HR'),
    ('CEO', 'HR Head'),
    ('CFO', 'HR Head'),
    ('CTO', 'HR Head'),
    ('CHRO', 'HR Head'),
    ('HR Head', 'CHRO'),
    ('Senior HR', 'HR Head'),
    ('Junior HR', 'Senior HR'),
    ('Project Owner', 'Senior HR'),
    ('Program Manager', 'Senior HR'),
    ('Delivery Manager', 'Senior HR'),
    ('Project Manager', 'Senior HR'),
    ('Team Lead', 'Senior HR'),
    ('Employee', 'Junior HR'),
    ('Intern', 'Junior HR'),
    ('Application Support Specialist', 'Junior HR'),
    ('IT Security', 'Junior HR'),
    ('IT Support Lead', 'Senior HR'),
    ('IT Support Manager', 'Senior HR'),
    ('Director', 'HR Head');

CREATE TEMPORARY TABLE ERM_HRBP_ACTIVE_CONTACTS (
    ROLE_NAME VARCHAR(100) NOT NULL PRIMARY KEY,
    USER_ID BIGINT NOT NULL
);

INSERT INTO ERM_HRBP_ACTIVE_CONTACTS (ROLE_NAME, USER_ID)
SELECT LOWER(contact_role.ROLE_NAME), MIN(contact.ID)
FROM ERM_USERS contact
JOIN ERM_USER_ROLES contact_roles ON contact_roles.USER_ID = contact.ID
JOIN ERM_ROLES contact_role ON contact_role.ID = contact_roles.ROLE_ID
WHERE contact.IS_ACTIVE = 1 AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
GROUP BY LOWER(contact_role.ROLE_NAME);

-- Use the primary designation assigned to each user to update existing HRBP links.
UPDATE ERM_USERS employee
JOIN ERM_ROLES designation_role ON designation_role.ID = employee.PRIMARY_ROLE_ID
JOIN ERM_HRBP_ROLE_MAPPING mapping
    ON LOWER(mapping.DESIGNATION_ROLE_NAME) = LOWER(designation_role.ROLE_NAME)
JOIN ERM_HRBP_ACTIVE_CONTACTS contacts
    ON contacts.ROLE_NAME = LOWER(mapping.ASSOCIATED_HR_ROLE_NAME)
SET employee.JUNIOR_HR_USER_ID = contacts.USER_ID;

-- Update all existing onboarding requests to use the HRBP required by their designation.
UPDATE ERM_ONBOARDING_REQUESTS onboarding
JOIN ERM_HRBP_ROLE_MAPPING mapping
    ON LOWER(mapping.DESIGNATION_ROLE_NAME) = LOWER(onboarding.DESIGNATION_ROLE_NAME)
JOIN ERM_HRBP_ACTIVE_CONTACTS contacts
    ON contacts.ROLE_NAME = LOWER(mapping.ASSOCIATED_HR_ROLE_NAME)
SET onboarding.JUNIOR_HR_USER_ID = contacts.USER_ID;

-- Refresh both current and requested HRBP links on saved employee data requests.
UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS request
JOIN ERM_USERS employee ON employee.ID = request.EMPLOYEE_USER_ID
SET request.CURRENT_JUNIOR_HR_USER_ID = employee.JUNIOR_HR_USER_ID;

UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS request
JOIN ERM_HRBP_ROLE_MAPPING mapping
    ON LOWER(mapping.DESIGNATION_ROLE_NAME) = LOWER(request.REQUESTED_DESIGNATION_ROLE_NAME)
JOIN ERM_HRBP_ACTIVE_CONTACTS contacts
    ON contacts.ROLE_NAME = LOWER(mapping.ASSOCIATED_HR_ROLE_NAME)
SET request.REQUESTED_JUNIOR_HR_USER_ID = contacts.USER_ID;

UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS request
JOIN ERM_USERS current_contact ON current_contact.ID = request.CURRENT_JUNIOR_HR_USER_ID
JOIN ERM_USERS requested_contact ON requested_contact.ID = request.REQUESTED_JUNIOR_HR_USER_ID
SET request.CURRENT_JUNIOR_HR_NAME = COALESCE(NULLIF(TRIM(current_contact.FULL_NAME), ''), current_contact.USERNAME),
    request.REQUESTED_JUNIOR_HR_NAME = COALESCE(NULLIF(TRIM(requested_contact.FULL_NAME), ''), requested_contact.USERNAME);

DROP TEMPORARY TABLE ERM_HRBP_ACTIVE_CONTACTS;
DROP TEMPORARY TABLE ERM_HRBP_ROLE_MAPPING;
