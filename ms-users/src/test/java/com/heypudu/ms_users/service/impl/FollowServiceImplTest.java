package com.heypudu.ms_users.service.impl;

import com.heypudu.ms_users.exception.ResourceNotFoundException;
import com.heypudu.ms_users.mapper.UserMapper;
import com.heypudu.ms_users.messaging.UserEventPublisher;
import com.heypudu.ms_users.model.User;
import com.heypudu.ms_users.repository.FollowRepository;
import com.heypudu.ms_users.repository.UserRepository;
import com.heypudu.ms_users.security.CognitoUserExtractor;
import com.heypudu.ms_users.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private S3Service s3Service;
    @Mock
    private CognitoUserExtractor cognitoUserExtractor;
    @Mock
    private UserEventPublisher userEventPublisher;

    private FollowServiceImpl followService;

    @BeforeEach
    void setUp() {
        followService = new FollowServiceImpl(
                followRepository,
                userRepository,
                userMapper,
                s3Service,
                cognitoUserExtractor,
                userEventPublisher);
    }

    @Test
    void isFollowingReturnsTrueWhenRelationshipExists() {
        UUID followerSub = UUID.randomUUID();
        UUID creatorSub = UUID.randomUUID();
        User follower = activeUser(followerSub);
        User creator = activeUser(creatorSub);

        when(userRepository.findByCognitoSub(creatorSub)).thenReturn(Optional.of(creator));
        when(userRepository.findByCognitoSub(followerSub)).thenReturn(Optional.of(follower));
        when(followRepository.existsByFollowerIdAndFollowedId(follower.getId(), creator.getId()))
                .thenReturn(true);

        assertTrue(followService.isFollowing(followerSub, creatorSub));
    }

    @Test
    void isFollowingReturnsFalseWhenRelationshipDoesNotExist() {
        UUID followerSub = UUID.randomUUID();
        UUID creatorSub = UUID.randomUUID();
        User follower = activeUser(followerSub);
        User creator = activeUser(creatorSub);

        when(userRepository.findByCognitoSub(creatorSub)).thenReturn(Optional.of(creator));
        when(userRepository.findByCognitoSub(followerSub)).thenReturn(Optional.of(follower));
        when(followRepository.existsByFollowerIdAndFollowedId(follower.getId(), creator.getId()))
                .thenReturn(false);

        assertFalse(followService.isFollowing(followerSub, creatorSub));
    }

    @Test
    void isFollowingReturnsFalseWhenFollowerDoesNotExist() {
        UUID followerSub = UUID.randomUUID();
        UUID creatorSub = UUID.randomUUID();
        User creator = activeUser(creatorSub);

        when(userRepository.findByCognitoSub(creatorSub)).thenReturn(Optional.of(creator));
        when(userRepository.findByCognitoSub(followerSub)).thenReturn(Optional.empty());

        assertFalse(followService.isFollowing(followerSub, creatorSub));
    }

    @Test
    void isFollowingThrowsWhenCreatorDoesNotExist() {
        UUID followerSub = UUID.randomUUID();
        UUID creatorSub = UUID.randomUUID();

        when(userRepository.findByCognitoSub(creatorSub)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> followService.isFollowing(followerSub, creatorSub));
    }

    private User activeUser(UUID cognitoSub) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setCognitoSub(cognitoSub);
        user.setStatus("ACTIVE");
        return user;
    }
}
