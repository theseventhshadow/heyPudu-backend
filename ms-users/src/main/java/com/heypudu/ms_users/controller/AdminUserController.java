package com.heypudu.ms_users.controller;

import com.heypudu.ms_users.dto.request.BanUserRequest;
import com.heypudu.ms_users.dto.response.UserProfileResponse;
import com.heypudu.ms_users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<UserProfileResponse>> listAllUsers(
            @PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable) {

        return ResponseEntity.ok(userService.listAllUsers(pageable));
    }

    @PutMapping("/{userId}/ban")
    public ResponseEntity<UserProfileResponse> banUser(
            @PathVariable UUID userId,
            @Valid @RequestBody BanUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(userService.banUser(userId, request, jwt));
    }

    @PutMapping("/{userId}/unban")
    public ResponseEntity<UserProfileResponse> unbanUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(userService.unbanUser(userId, jwt));
    }

    @PutMapping("/{userId}/verify")
    public ResponseEntity<UserProfileResponse> verifyUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(userService.verifyUser(userId, jwt));
    }

    @PutMapping("/{userId}/unverify")
    public ResponseEntity<UserProfileResponse> unverifyUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(userService.unverifyUser(userId, jwt));
    }
}