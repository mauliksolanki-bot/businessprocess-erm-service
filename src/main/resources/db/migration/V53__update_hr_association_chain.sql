-- Keep the assigned HR contact aligned with the requested HR reporting chain.
UPDATE ERM_USERS u
JOIN ERM_ROLES designation_role ON designation_role.ID = u.PRIMARY_ROLE_ID
SET u.JUNIOR_HR_USER_ID = CASE LOWER(designation_role.ROLE_NAME)
    WHEN 'senior hr' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'hr head' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    WHEN 'hr head' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'chro' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    WHEN 'chro' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'hr head' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    ELSE u.JUNIOR_HR_USER_ID
END
WHERE LOWER(designation_role.ROLE_NAME) IN ('senior hr', 'hr head', 'chro');

UPDATE ERM_ONBOARDING_REQUESTS o
SET o.JUNIOR_HR_USER_ID = CASE LOWER(o.DESIGNATION_ROLE_NAME)
    WHEN 'senior hr' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'hr head' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    WHEN 'hr head' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'chro' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    WHEN 'chro' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'hr head' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    ELSE o.JUNIOR_HR_USER_ID
END
WHERE LOWER(o.DESIGNATION_ROLE_NAME) IN ('senior hr', 'hr head', 'chro');

UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS p
JOIN ERM_USERS employee ON employee.ID = p.EMPLOYEE_USER_ID
SET p.CURRENT_JUNIOR_HR_USER_ID = employee.JUNIOR_HR_USER_ID;

UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS p
SET p.REQUESTED_JUNIOR_HR_USER_ID = CASE LOWER(p.REQUESTED_DESIGNATION_ROLE_NAME)
    WHEN 'senior hr' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'hr head' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    WHEN 'hr head' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'chro' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    WHEN 'chro' THEN (
        SELECT MIN(contact.ID) FROM ERM_USERS contact
        JOIN ERM_USER_ROLES ur ON ur.USER_ID = contact.ID
        JOIN ERM_ROLES role ON role.ID = ur.ROLE_ID
        WHERE LOWER(role.ROLE_NAME) = 'hr head' AND contact.IS_ACTIVE = 1
          AND LOWER(contact.EMPLOYMENT_STATUS) = 'active'
    )
    ELSE p.REQUESTED_JUNIOR_HR_USER_ID
END
WHERE LOWER(p.REQUESTED_DESIGNATION_ROLE_NAME) IN ('senior hr', 'hr head', 'chro');

UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS p
JOIN ERM_USERS current_contact ON current_contact.ID = p.CURRENT_JUNIOR_HR_USER_ID
JOIN ERM_USERS requested_contact ON requested_contact.ID = p.REQUESTED_JUNIOR_HR_USER_ID
SET p.CURRENT_JUNIOR_HR_NAME = COALESCE(NULLIF(TRIM(current_contact.FULL_NAME), ''), current_contact.USERNAME),
    p.REQUESTED_JUNIOR_HR_NAME = COALESCE(NULLIF(TRIM(requested_contact.FULL_NAME), ''), requested_contact.USERNAME);
