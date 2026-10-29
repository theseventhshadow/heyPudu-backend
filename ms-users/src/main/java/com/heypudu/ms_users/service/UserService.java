package com.heypudu.ms_users.service;

import com.heypudu.ms_users.dto.request.BanUserRequest;
import com.heypudu.ms_users.dto.request.CreateUserProfileRequest;
import com.heypudu.ms_users.dto.request.PresignedUrlRequest;
import com.heypudu.ms_users.dto.request.UpdateProfileRequest;
import com.heypudu.ms_users.dto.response.PresignedUrlResponse;
import com.heypudu.ms_users.dto.response.UserProfileResponse;
import com.heypudu.ms_users.dto.response.UserPublicResponse;
import com.heypudu.ms_users.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getCurrentUserProfile(Jwt jwt);

    UserProfileResponse updateCurrentUserProfile(UpdateProfileRequest request, Jwt jwt);

    void deleteCurrentUser(Jwt jwt);

    UserPublicResponse getPublicProfile(UUID userId, Jwt jwt);

    Page<UserSummaryResponse> searchUsers(String query, Pageable pageable);

    UserSummaryResponse createUserProfile(CreateUserProfileRequest request);

    UserSummaryResponse getUserSummary(UUID userId);

    UserSummaryResponse getUserSummaryByCognitoSub(UUID cognitoSub);

    String getAvatarRedirectUrlByCognitoSub(UUID cognitoSub);

    PresignedUrlResponse generateAvatarUploadUrl(PresignedUrlRequest request, Jwt jwt);

    UserProfileResponse banUser(UUID userId, BanUserRequest request, Jwt jwt);

    UserProfileResponse unbanUser(UUID userId, Jwt jwt);

    UserProfileResponse verifyUser(UUID userId, Jwt jwt);

    UserProfileResponse unverifyUser(UUID userId, Jwt jwt);

    Page<UserProfileResponse> listAllUsers(Pageable pageable);
}