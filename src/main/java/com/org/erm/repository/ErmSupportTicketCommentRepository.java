package com.org.erm.repository;

import com.org.erm.model.ErmSupportTicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmSupportTicketCommentRepository extends JpaRepository<ErmSupportTicketComment, Long> {

    List<ErmSupportTicketComment> findAllByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);
}
