package com.org.erm.controller;

import com.org.erm.dto.response.UserProfileResponse;
import com.org.erm.dto.response.UserMentionOptionResponse;
import com.org.erm.dto.response.UserMentionNotificationResponse;
import com.org.erm.service.MentionNotificationService;
import com.org.erm.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;
    private final MentionNotificationService mentionNotificationService;

    public UserController(UserService userService, MentionNotificationService mentionNotificationService) {
        this.userService = userService;
        this.mentionNotificationService = mentionNotificationService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(authentication.getName()));
    }

    @GetMapping("/mentions")
    public ResponseEntity<List<UserMentionOptionResponse>> searchMentionableUsers(@RequestParam(name = "query", required = false) String query) {
        return ResponseEntity.ok(userService.searchMentionableUsers(query));
    }

    @GetMapping("/mention-notifications")
    public ResponseEntity<List<UserMentionNotificationResponse>> getMentionNotifications(Authentication authentication) {
        return ResponseEntity.ok(mentionNotificationService.listForUser(authentication.getName()));
    }
}
