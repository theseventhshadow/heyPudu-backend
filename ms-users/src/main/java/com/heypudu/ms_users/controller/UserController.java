package com.heypudu.ms_users.controller;

import com.heypudu.ms_users.dto.request.PresignedUrlRequest;
import com.heypudu.ms_users.dto.request.UpdateProfileRequest;
import com.heypudu.ms_users.dto.response.PresignedUrlResponse;
import com.heypudu.ms_users.dto.response.UserProfileResponse;
import com.heypudu.ms_users.dto.response.UserPublicResponse;
import com.heypudu.ms_users.dto.response.UserSummaryResponse;
import com.heypudu.ms_users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(jwt));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(userService.updateCurrentUserProfile(request, jwt));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        userService.deleteCurrentUser(jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserPublicResponse> getPublicProfile(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(userService.getPublicProfile(userId, jwt));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserSummaryResponse>> searchUsers(
            @RequestParam("q") String query,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(userService.searchUsers(query, pageable));
    }

    @PostMapping("/me/avatar/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generateAvatarUploadUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(userService.generateAvatarUploadUrl(request, jwt));
    }
}