-- Senior HR is associated with the next role in the hierarchy: HR Head.
UPDATE ERM_USERS u
JOIN ERM_USER_ROLES ur ON ur.USER_ID = u.ID
JOIN ERM_ROLES r ON r.ID = ur.ROLE_ID AND LOWER(r.ROLE_NAME) = 'senior hr'
SET u.JUNIOR_HR_USER_ID = (
    SELECT MIN(head.ID)
    FROM ERM_USERS head
    JOIN ERM_USER_ROLES head_ur ON head_ur.USER_ID = head.ID
    JOIN ERM_ROLES head_role ON head_role.ID = head_ur.ROLE_ID
    WHERE LOWER(head_role.ROLE_NAME) = 'hr head'
      AND head.IS_ACTIVE = 1
      AND LOWER(head.EMPLOYMENT_STATUS) = 'active'
);

UPDATE ERM_ONBOARDING_REQUESTS o
SET o.JUNIOR_HR_USER_ID = (
    SELECT MIN(head.ID)
    FROM ERM_USERS head
    JOIN ERM_USER_ROLES head_ur ON head_ur.USER_ID = head.ID
    JOIN ERM_ROLES head_role ON head_role.ID = head_ur.ROLE_ID
    WHERE LOWER(head_role.ROLE_NAME) = 'hr head'
      AND head.IS_ACTIVE = 1
      AND LOWER(head.EMPLOYMENT_STATUS) = 'active'
)
WHERE LOWER(o.DESIGNATION_ROLE_NAME) = 'senior hr';

UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS p
JOIN ERM_USERS employee ON employee.ID = p.EMPLOYEE_USER_ID
SET p.CURRENT_JUNIOR_HR_USER_ID = employee.JUNIOR_HR_USER_ID;

UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS p
SET p.REQUESTED_JUNIOR_HR_USER_ID = (
        SELECT MIN(head.ID)
        FROM ERM_USERS head
        JOIN ERM_USER_ROLES head_ur ON head_ur.USER_ID = head.ID
        JOIN ERM_ROLES head_role ON head_role.ID = head_ur.ROLE_ID
        WHERE LOWER(head_role.ROLE_NAME) = 'hr head'
          AND head.IS_ACTIVE = 1
          AND LOWER(head.EMPLOYMENT_STATUS) = 'active'
    )
WHERE LOWER(p.REQUESTED_DESIGNATION_ROLE_NAME) = 'senior hr';

UPDATE ERM_EMPLOYEE_PROFILE_UPDATE_REQUESTS p
JOIN ERM_USERS current_contact ON current_contact.ID = p.CURRENT_JUNIOR_HR_USER_ID
JOIN ERM_USERS requested_contact ON requested_contact.ID = p.REQUESTED_JUNIOR_HR_USER_ID
SET p.CURRENT_JUNIOR_HR_NAME = COALESCE(NULLIF(TRIM(current_contact.FULL_NAME), ''), current_contact.USERNAME),
    p.REQUESTED_JUNIOR_HR_NAME = COALESCE(NULLIF(TRIM(requested_contact.FULL_NAME), ''), requested_contact.USERNAME);
