package com.org.erm.repository;

import com.org.erm.model.ErmNavMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ErmNavMenuRepository extends JpaRepository<ErmNavMenu, Long> {

    @Query(
            value = """
                    SELECT DISTINCT m.*
                    FROM ERM_NAV_MENUS m
                    JOIN ERM_ROLE_NAV_MENUS rm ON rm.MENU_ID = m.ID
                    JOIN ERM_USER_ROLES ur ON ur.ROLE_ID = rm.ROLE_ID
                    JOIN ERM_USERS u ON u.ID = ur.USER_ID
                    WHERE LOWER(u.USERNAME) = LOWER(:username)
                      AND m.IS_ACTIVE = 1
                    ORDER BY m.SORT_ORDER
                    """,
            nativeQuery = true
    )
    List<ErmNavMenu> findAuthorizedMenusByUsername(@Param("username") String username);
}
