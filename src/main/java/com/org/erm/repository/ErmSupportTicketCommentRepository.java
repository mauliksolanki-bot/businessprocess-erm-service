package com.org.erm.repository;

import com.org.erm.model.ErmSupportTicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmSupportTicketCommentRepository extends JpaRepository<ErmSupportTicketComment, Long> {

    List<ErmSupportTicketComment> findAllByTicketIdOrderByCreatedAtAscIdAsc(Long ticketId);

    Optional<ErmSupportTicketComment> findByGithubCommentId(Long githubCommentId);

    boolean existsByGithubDeliveryId(String githubDeliveryId);
}
