package com.heypudu.ms_users.controller;

import com.heypudu.ms_users.dto.response.NotificationResponse;
import com.heypudu.ms_users.security.CognitoUserExtractor;
import com.heypudu.ms_users.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CognitoUserExtractor cognitoUserExtractor;

    public NotificationController(NotificationService notificationService,
                                  CognitoUserExtractor cognitoUserExtractor) {
        this.notificationService = notificationService;
        this.cognitoUserExtractor = cognitoUserExtractor;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        UUID recipientId = cognitoUserExtractor.extractSub(jwt);
        return ResponseEntity.ok(notificationService.list(recipientId, unreadOnly, pageable));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        notificationService.markAsRead(
                notificationId,
                cognitoUserExtractor.extractSub(jwt)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(cognitoUserExtractor.extractSub(jwt));
        return ResponseEntity.noContent().build();
    }
}
