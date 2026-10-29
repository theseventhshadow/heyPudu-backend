package com.heypudu.ms_users.service.impl;

import com.heypudu.ms_users.client.AuthClient;
import com.heypudu.ms_users.client.ModerationClient;
import com.heypudu.ms_users.dto.response.AuthorReportCountResponse;
import com.heypudu.ms_users.dto.response.BanCandidateResponse;
import com.heypudu.ms_users.dto.request.BanUserRequest;
import com.heypudu.ms_users.dto.request.CreateUserProfileRequest;
import com.heypudu.ms_users.dto.request.PresignedUrlRequest;
import com.heypudu.ms_users.dto.request.UpdateProfileRequest;
import com.heypudu.ms_users.dto.response.PresignedUrlResponse;
import com.heypudu.ms_users.dto.response.UserProfileResponse;
import com.heypudu.ms_users.dto.response.UserPublicResponse;
import com.heypudu.ms_users.dto.response.UserSummaryResponse;
import com.heypudu.ms_users.exception.BusinessException;
import com.heypudu.ms_users.exception.ResourceNotFoundException;
import com.heypudu.ms_users.mapper.UserMapper;
import com.heypudu.ms_users.model.User;
import com.heypudu.ms_users.repository.UserRepository;
import com.heypudu.ms_users.security.CognitoUserExtractor;
import com.heypudu.ms_users.service.S3Service;
import com.heypudu.ms_users.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_BANNED = "BANNED";
    private static final String STATUS_DELETED = "DELETED";

    private static final Duration USERNAME_CHANGE_COOLDOWN = Duration.ofDays(30);
    private static final String ANONYMOUS_NAME = "Usuario eliminado";
    private static final String ANONYMOUS_USERNAME_PREFIX = "deleted_";

    private final UserRepository userRepository;
    private final AuthClient authClient;
    private final S3Service s3Service;
    private final UserMapper userMapper;
    private final CognitoUserExtractor cognitoUserExtractor;
    private final ModerationClient moderationClient;

    public UserServiceImpl(UserRepository userRepository,
                           AuthClient authClient,
                           S3Service s3Service,
                           UserMapper userMapper,
                           CognitoUserExtractor cognitoUserExtractor,
                           ModerationClient moderationClient) {
        this.userRepository = userRepository;
        this.authClient = authClient;
        this.s3Service = s3Service;
        this.userMapper = userMapper;
        this.cognitoUserExtractor = cognitoUserExtractor;
        this.moderationClient = moderationClient;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(Jwt jwt) {
        User user = findCurrentUser(jwt);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UpdateProfileRequest request, Jwt jwt) {
        User user = findCurrentUser(jwt);

        if (!user.getUsername().equals(request.username())) {
            validateUsernameChange(user);
            if (userRepository.existsByUsername(request.username())) {
                throw new BusinessException("El username ya esta en uso");
            }
            user.setUsername(request.username());
            user.setUsernameUpdatedAt(Instant.now());
        }

        user.setName(request.name());
        user.setBio(request.bio());

        if (request.avatarKey() == null) {
            if (user.getAvatarKey() != null) {
                s3Service.deleteAvatar(user.getAvatarKey());
                user.setAvatarKey(null);
            }
        } else if (!request.avatarKey().equals(user.getAvatarKey())) {
            if (user.getAvatarKey() != null) {
                s3Service.deleteAvatar(user.getAvatarKey());
            }
            user.setAvatarKey(request.avatarKey());
        }

        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCurrentUser(Jwt jwt) {
        User user = findCurrentUser(jwt);

        anonymize(user);
        user.setStatus(STATUS_DELETED);
        user.setAnonymizedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        authClient.deleteUser(user.getCognitoSub(), jwt.getTokenValue());
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicResponse getPublicProfile(UUID userId, Jwt jwt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + userId));

        if (STATUS_DELETED.equals(user.getStatus())) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + userId);
        }

        String avatarUrl = user.getAvatarKey() != null
                ? s3Service.generateAvatarDownloadUrl(user.getAvatarKey())
                : null;

        return userMapper.toPublicResponse(user, avatarUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> searchUsers(String query, Pageable pageable) {
        Page<User> users = userRepository.searchByNameOrUsername(query, pageable);
        return userMapper.toSummaryResponsePage(users, this::resolveAvatarUrl);
    }

    @Override
    @Transactional
    public UserSummaryResponse createUserProfile(CreateUserProfileRequest request) {
        return userRepository.findByCognitoSub(request.cognitoSub())
                .map(existing -> userMapper.toSummaryResponse(existing, resolveAvatarUrl(existing)))
                .orElseGet(() -> {
                    if (userRepository.existsByUsername(request.username())) {
                        throw new BusinessException("El username ya esta en uso");
                    }

                    User user = new User(
                            request.cognitoSub(),
                            request.name(),
                            request.username(),
                            request.bio(),
                            request.avatarKey(),
                            Instant.now()
                    );

                    User saved = userRepository.save(user);
                    return userMapper.toSummaryResponse(saved, resolveAvatarUrl(saved));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + userId));
        return userMapper.toSummaryResponse(user, resolveAvatarUrl(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserSummaryByCognitoSub(UUID cognitoSub) {
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado para cognito_sub: " + cognitoSub));
        return userMapper.toSummaryResponse(user, resolveAvatarUrl(user));
    }

    @Override
    @Transactional(readOnly = true)
    public String getAvatarRedirectUrlByCognitoSub(UUID cognitoSub) {
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado para cognito_sub: " + cognitoSub));
        if (user.getAvatarKey() == null) {
            throw new ResourceNotFoundException("El usuario no tiene avatar");
        }
        return s3Service.generateAvatarDownloadUrl(user.getAvatarKey());
    }

    @Override
    @Transactional
    public void updatePostCount(UUID cognitoSub, int delta) {
        User user = userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado para cognito_sub: " + cognitoSub));
        userRepository.updatePostCount(user.getId(), delta);
    }

    @Override
    public PresignedUrlResponse generateAvatarUploadUrl(PresignedUrlRequest request, Jwt jwt) {
        return s3Service.generateAvatarUploadUrl(request);
    }

    @Override
    @Transactional
    public UserProfileResponse banUser(UUID userId, BanUserRequest request, Jwt jwt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + userId));

        if (STATUS_DELETED.equals(user.getStatus())) {
            throw new BusinessException("No se puede banear a un usuario eliminado");
        }

        if (STATUS_BANNED.equals(user.getStatus())) {
            return toProfileResponse(user);
        }

        user.setStatus(STATUS_BANNED);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);

        authClient.disableUser(user.getCognitoSub(), jwt.getTokenValue());

        return toProfileResponse(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse unbanUser(UUID userId, Jwt jwt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + userId));

        if (!STATUS_BANNED.equals(user.getStatus())) {
            throw new BusinessException("El usuario no esta baneado");
        }

        user.setStatus(STATUS_ACTIVE);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);

        authClient.enableUser(user.getCognitoSub(), jwt.getTokenValue());

        return toProfileResponse(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse verifyUser(UUID userId, Jwt jwt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + userId));

        user.setVerified(true);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse unverifyUser(UUID userId, Jwt jwt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + userId));

        user.setVerified(false);
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> listAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::toProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BanCandidateResponse> listBanCandidates(int threshold, Jwt jwt) {
        String token = jwt.getTokenValue();

        Map<UUID, Long> postCounts = toCountMap(moderationClient.getPostReportCounts(token));
        Map<UUID, Long> commentCounts = toCountMap(moderationClient.getCommentReportCounts(token));

        Set<UUID> authorIds = new HashSet<>(postCounts.keySet());
        authorIds.addAll(commentCounts.keySet());

        List<BanCandidateResponse> candidates = new ArrayList<>();
        for (UUID authorId : authorIds) {
            long fromPosts = postCounts.getOrDefault(authorId, 0L);
            long fromComments = commentCounts.getOrDefault(authorId, 0L);
            long total = fromPosts + fromComments;
            if (total < threshold) {
                continue;
            }
            userRepository.findByCognitoSub(authorId).ifPresent(user ->
                    candidates.add(new BanCandidateResponse(
                            toProfileResponse(user), total, fromPosts, fromComments)));
        }

        candidates.sort(Comparator.comparingLong(BanCandidateResponse::totalReports).reversed());
        return candidates;
    }

    private Map<UUID, Long> toCountMap(List<AuthorReportCountResponse> counts) {
        Map<UUID, Long> map = new HashMap<>();
        for (AuthorReportCountResponse count : counts) {
            map.merge(count.authorId(), count.totalReports(), Long::sum);
        }
        return map;
    }

    private User findCurrentUser(Jwt jwt) {
        UUID cognitoSub = cognitoUserExtractor.extractSub(jwt);
        return userRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado para cognito_sub: " + cognitoSub));
    }

    private void validateUsernameChange(User user) {
        Instant lastChange = user.getUsernameUpdatedAt();
        if (lastChange == null) {
            return;
        }
        Instant nextAllowed = lastChange.plus(USERNAME_CHANGE_COOLDOWN);
        if (Instant.now().isBefore(nextAllowed)) {
            throw new BusinessException(
                    "Solo puedes cambiar tu nombre de usuario una vez cada 30 dias");
        }
    }

    private void anonymize(User user) {
        user.setName(ANONYMOUS_NAME);
        user.setUsername(ANONYMOUS_USERNAME_PREFIX + user.getCognitoSub().toString().substring(0, 8));
        user.setBio(null);
        user.setAvatarKey(null);
        user.setVerified(false);
    }

    private UserProfileResponse toProfileResponse(User user) {
        String avatarUrl = user.getAvatarKey() != null
                ? s3Service.generateAvatarDownloadUrl(user.getAvatarKey())
                : null;
        return userMapper.toProfileResponse(user, avatarUrl);
    }

    private String resolveAvatarUrl(User user) {
        return user.getAvatarKey() != null
                ? s3Service.generateAvatarDownloadUrl(user.getAvatarKey())
                : null;
    }
}