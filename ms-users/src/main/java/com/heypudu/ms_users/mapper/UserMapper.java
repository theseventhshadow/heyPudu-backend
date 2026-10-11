package com.heypudu.ms_users.mapper;

import com.heypudu.ms_users.dto.response.FollowResponse;
import com.heypudu.ms_users.dto.response.UserProfileResponse;
import com.heypudu.ms_users.dto.response.UserPublicResponse;
import com.heypudu.ms_users.dto.response.UserSummaryResponse;
import com.heypudu.ms_users.model.Follow;
import com.heypudu.ms_users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class UserMapper {

    private static final String STATUS_BANNED = "BANNED";

    public UserProfileResponse toProfileResponse(User user, String avatarUrl) {
        return new UserProfileResponse(
                user.getId(),
                user.getCognitoSub(),
                user.getName(),
                user.getUsername(),
                user.getBio(),
                avatarUrl,
                user.isVerified(),
                user.getStatus(),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.getPostCount(),
                user.getUsernameUpdatedAt(),
                user.getAnonymizedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public UserPublicResponse toPublicResponse(User user, String avatarUrl) {
        return new UserPublicResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getBio(),
                avatarUrl,
                user.isVerified(),
                STATUS_BANNED.equals(user.getStatus()),
                user.getFollowerCount(),
                user.getFollowingCount(),
                user.getPostCount(),
                user.getCreatedAt()
        );
    }

    public UserSummaryResponse toSummaryResponse(User user, String avatarUrl) {
        return new UserSummaryResponse(
                user.getId(),
                user.getCognitoSub(),
                user.getName(),
                user.getUsername(),
                avatarUrl,
                user.isVerified(),
                user.getStatus()
        );
    }

    public FollowResponse toFollowResponse(Follow follow) {
        return new FollowResponse(
                follow.getId(),
                follow.getFollowerId(),
                follow.getFollowedId(),
                follow.getCreatedAt()
        );
    }

    public Page<UserPublicResponse> toPublicResponsePage(
            Page<User> users,
            Function<User, String> avatarUrlResolver) {

        return users.map(user -> toPublicResponse(user, avatarUrlResolver.apply(user)));
    }

    public Page<UserSummaryResponse> toSummaryResponsePage(
            Page<User> users,
            Function<User, String> avatarUrlResolver) {

        return users.map(user -> toSummaryResponse(user, avatarUrlResolver.apply(user)));
    }
}