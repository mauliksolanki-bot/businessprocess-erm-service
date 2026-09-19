package com.org.erm.service;

import com.org.erm.dto.response.UserMentionNotificationResponse;
import com.org.erm.model.ErmMentionNotification;
import com.org.erm.model.ErmUser;
import com.org.erm.repository.ErmMentionNotificationRepository;
import com.org.erm.repository.ErmUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MentionNotificationService {

    private static final Pattern USERNAME_MENTION_PATTERN = Pattern.compile("(^|\\s|\\()@([A-Za-z0-9._-]+)");

    private final ErmMentionNotificationRepository mentionNotificationRepository;
    private final ErmUserRepository userRepository;

    public MentionNotificationService(ErmMentionNotificationRepository mentionNotificationRepository,
                                      ErmUserRepository userRepository) {
        this.mentionNotificationRepository = mentionNotificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notifyMentions(String actorUsername,
                               String commentText,
                               String contextType,
                               Long contextId,
                               String message,
                               String href) {
        if (!StringUtils.hasText(actorUsername) || !StringUtils.hasText(commentText) || !StringUtils.hasText(message) || !StringUtils.hasText(href)) {
            return;
        }

        Set<String> mentionedUsernames = extractMentionedUsernames(commentText);
        if (mentionedUsernames.isEmpty()) {
            return;
        }

        String actorKey = actorUsername.trim().toLowerCase(Locale.ROOT);
        List<ErmUser> recipients = userRepository.findActiveUsersByUsernames(mentionedUsernames);
        for (ErmUser recipient : recipients) {
            String recipientUsername = recipient.getUsername();
            if (!StringUtils.hasText(recipientUsername)) {
                continue;
            }
            if (recipientUsername.trim().toLowerCase(Locale.ROOT).equals(actorKey)) {
                continue;
            }
            ErmMentionNotification notification = new ErmMentionNotification();
            notification.setRecipientUsername(recipientUsername.trim());
            notification.setActorUsername(actorUsername.trim());
            notification.setContextType(contextType);
            notification.setContextId(contextId);
            notification.setMessageText(message);
            notification.setHref(href);
            mentionNotificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public List<UserMentionNotificationResponse> listForUser(String username) {
        if (!StringUtils.hasText(username)) {
            return List.of();
        }
        return mentionNotificationRepository.findTop50ByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(username.trim()).stream()
                .map(notification -> new UserMentionNotificationResponse(
                        notification.getId(),
                        notification.getActorUsername(),
                        notification.getContextType(),
                        notification.getContextId(),
                        notification.getMessageText(),
                        notification.getHref(),
                        notification.getCreatedAt(),
                        notification.getReadAt()
                ))
                .toList();
    }

    @Transactional
    public void notifyUsers(Set<String> recipientUsernames,
                            String actorUsername,
                            String contextType,
                            Long contextId,
                            String message,
                            String href) {
        if (recipientUsernames == null || recipientUsernames.isEmpty() || !StringUtils.hasText(actorUsername)
                || !StringUtils.hasText(message) || !StringUtils.hasText(href)) {
            return;
        }

        for (String recipientUsername : recipientUsernames) {
            if (!StringUtils.hasText(recipientUsername)) {
                continue;
            }
            ErmMentionNotification notification = new ErmMentionNotification();
            notification.setRecipientUsername(recipientUsername.trim());
            notification.setActorUsername(actorUsername.trim());
            notification.setContextType(contextType);
            notification.setContextId(contextId);
            notification.setMessageText(message);
            notification.setHref(href);
            mentionNotificationRepository.save(notification);
        }
    }

    private Set<String> extractMentionedUsernames(String text) {
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = USERNAME_MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String username = matcher.group(2);
            if (StringUtils.hasText(username)) {
                usernames.add(username.trim().toLowerCase(Locale.ROOT));
            }
        }
        return usernames;
    }
}
