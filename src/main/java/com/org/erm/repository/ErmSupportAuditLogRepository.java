package com.org.erm.repository;

import com.org.erm.model.ErmSupportAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErmSupportAuditLogRepository extends JpaRepository<ErmSupportAuditLog, Long> {

    boolean existsByTicketIdAndActionType(Long ticketId, String actionType);
}
