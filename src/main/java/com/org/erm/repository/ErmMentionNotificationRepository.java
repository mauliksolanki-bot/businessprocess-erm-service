package com.org.erm.repository;

import com.org.erm.model.ErmMentionNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmMentionNotificationRepository extends JpaRepository<ErmMentionNotification, Long> {

    List<ErmMentionNotification> findTop50ByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(String recipientUsername);
}
