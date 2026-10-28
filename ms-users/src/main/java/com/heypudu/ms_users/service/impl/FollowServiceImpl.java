package com.heypudu.ms_users.service.impl;

import com.heypudu.ms_users.dto.response.FollowResponse;
import com.heypudu.ms_users.dto.response.UserSummaryResponse;
import com.heypudu.ms_users.exception.BusinessException;
import com.heypudu.ms_users.exception.ResourceNotFoundException;
import com.heypudu.ms_users.mapper.UserMapper;
import com.heypudu.ms_users.model.Follow;
import com.heypudu.ms_users.model.User;
import com.heypudu.ms_users.repository.FollowRepository;
import com.heypudu.ms_users.repository.UserRepository;
import com.heypudu.ms_users.security.CognitoUserExtractor;
import com.heypudu.ms_users.service.FollowService;
import com.heypudu.ms_users.service.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FollowServiceImpl implements FollowService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int DELTA_UP = 1;
    private static final int DELTA_DOWN = -1;

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final S3Service s3Service;
    private final CognitoUserExtractor cognitoUserExtractor;

    public FollowServiceImpl(FollowRepository followRepository,
                             UserRepository userRepository,
                             UserMapper userMapper,
                             S3Service s3Service,
                             CognitoUserExtractor cognitoUserExtractor) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.s3Service = s3Service;
        this.cognitoUserExtractor = cognitoUserExtractor;
    }

    @Override
    @Transactional
    public FollowResponse follow(UUID followedId, Jwt jwt) {
        UUID cognitoSub = cognitoUserExtractor.extractSub(jwt);
        User follower = findActiveUserByCognitoSub(cognitoSub);
        User followed = findActiveUserById(followedId);

        if (follower.getId().equals(followed.getId())) {
            throw new BusinessException("No puedes seguirte a ti mismo");
        }

        Follow existing = followRepository
                .findByFollowerIdAndFollowedId(follower.getId(), followed.getId())
                .orElse(null);

        if (existing != null) {
            return userMapper.toFollowResponse(existing);
        }

        Follow follow = new Follow(follower.getId(), followed.getId(), Instant.now());
        Follow saved = followRepository.save(follow);

        userRepository.updateFollowingCount(follower.getId(), DELTA_UP);
        userRepository.updateFollowerCount(followed.getId(), DELTA_UP);

        return userMapper.toFollowResponse(saved);
    }

    @Override
    @Transactional
    public void unfollow(UUID followedId, Jwt jwt) {
        UUID cognitoSub = cognitoUserExtractor.extractSub(jwt);
        User follower = findActiveUserByCognitoSub(cognitoSub);
        User followed = findActiveUserById(followedId);

        Follow existing = followRepository
                .findByFollowerIdAndFollowedId(follower.getId(), followed.getId())
                .orElse(null);

        if (existing == null) {
            return;
        }

        followRepository.delete(existing);

        userRepository.updateFollowingCount(follower.getId(), DELTA_DOWN);
        userRepository.updateFollowerCount(followed.getId(), DELTA_DOWN);
    }

       @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listFollowers(UUID userId, Pageable pageable) {
        Page<User> users = followRepository.findFollowersAsUsers(userId, pageable);
        return userMapper.toSummaryResponsePage(users, this::resolveAvatarUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listFollowing(UUID userId, Pageable pageable) {
        Page<User> users = followRepository.findFollowingAsUsers(userId, pageable);
        return userMapper.toSummaryResponsePage(users, this::resolveAvatarUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> listFollowingIds(UUID userId) {
        return followRepository.findFollowedIdsByFollowerId(userId);
    }

    private User findActiveUserByCognitoSub(UUID cognitoSub) {
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado para cognito_sub: " + cognitoSub));
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("El usuario no esta activo");
        }
        return user;
    }

    private User findActiveUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + userId));
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("El usuario no esta activo");
        }
        return user;
    }

    private String resolveAvatarUrl(User user) {
        return user.getAvatarKey() != null
                ? s3Service.generateAvatarDownloadUrl(user.getAvatarKey())
                : null;
    }
}