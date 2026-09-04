package com.org.erm.repository;

import com.org.erm.model.ErmSupportTicket;
import com.org.erm.model.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ErmSupportTicketRepository extends JpaRepository<ErmSupportTicket, Long> {

    Optional<ErmSupportTicket> findByTicketNumber(String ticketNumber);

    List<ErmSupportTicket> findAllByRequesterUserIdOrderByCreatedAtDesc(Long requesterUserId);

    List<ErmSupportTicket> findAllByAssigneeUserIdOrderByCreatedAtDesc(Long assigneeUserId);

    List<ErmSupportTicket> findAllByQueueIdInOrderByCreatedAtDesc(List<Long> queueIds);

    long countByAssigneeUserIdAndStatusIn(Long assigneeUserId, List<SupportTicketStatus> statuses);

    long countByQueueIdAndStatusIn(Long queueId, Collection<SupportTicketStatus> statuses);
}
