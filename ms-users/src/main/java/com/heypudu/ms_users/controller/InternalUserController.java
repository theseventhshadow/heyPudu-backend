package com.heypudu.ms_users.controller;

import com.heypudu.ms_users.dto.request.CreateUserProfileRequest;
import com.heypudu.ms_users.dto.response.UserSummaryResponse;
import com.heypudu.ms_users.service.FollowService;
import com.heypudu.ms_users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserService userService;
    private final FollowService followService;

    public InternalUserController(UserService userService, FollowService followService) {
        this.userService = userService;
        this.followService = followService;
    }

    @PostMapping
    public ResponseEntity<UserSummaryResponse> createUserProfile(
            @Valid @RequestBody CreateUserProfileRequest request) {

        UserSummaryResponse response = userService.createUserProfile(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/summary")
    public ResponseEntity<UserSummaryResponse> getUserSummary(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserSummary(userId));
    }

    @GetMapping("/{userId}/following/ids")
    public ResponseEntity<List<UUID>> listFollowingIds(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.listFollowingIds(userId));
    }

    @GetMapping("/{userId}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable UUID userId) {
        try {
            userService.getUserSummary(userId);
            return ResponseEntity.ok(true);
        } catch (Exception ex) {
            return ResponseEntity.ok(false);
        }
    }
}