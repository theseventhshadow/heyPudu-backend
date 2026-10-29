package com.heypudu.ms_users.service;

import com.heypudu.ms_users.dto.response.FollowResponse;
import com.heypudu.ms_users.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

public interface FollowService {

    FollowResponse follow(UUID followedId, Jwt jwt);

    void unfollow(UUID followedId, Jwt jwt);

    Page<UserSummaryResponse> listFollowers(UUID userId, Pageable pageable);

    Page<UserSummaryResponse> listFollowing(UUID userId, Pageable pageable);

    List<UUID> listFollowingIds(UUID userId);

    List<UUID> listFollowingCognitoSubs(UUID cognitoSub);
}