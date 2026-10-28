package com.heypudu.ms_auth.service.impl;

import com.heypudu.ms_auth.client.UserClient;
import com.heypudu.ms_auth.dto.request.CreateUserProfileRequest;
import com.heypudu.ms_auth.dto.response.AuthUserResponse;
import com.heypudu.ms_auth.dto.response.SyncUserResponse;
import com.heypudu.ms_auth.exception.ResourceNotFoundException;
import com.heypudu.ms_auth.mapper.AuthUserMapper;
import com.heypudu.ms_auth.model.AuthUser;
import com.heypudu.ms_auth.repository.AuthUserRepository;
import com.heypudu.ms_auth.security.CognitoUserExtractor;
import com.heypudu.ms_auth.service.AuthService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String DEFAULT_NAME = "Usuario";
    private static final String DEFAULT_USERNAME_PREFIX = "user_";

    private final AuthUserRepository authUserRepository;
    private final CognitoUserExtractor cognitoUserExtractor;
    private final AuthUserMapper authUserMapper;
    private final UserClient userClient;

    public AuthServiceImpl(AuthUserRepository authUserRepository,
                           CognitoUserExtractor cognitoUserExtractor,
                           AuthUserMapper authUserMapper,
                           UserClient userClient) {
        this.authUserRepository = authUserRepository;
        this.cognitoUserExtractor = cognitoUserExtractor;
        this.authUserMapper = authUserMapper;
        this.userClient = userClient;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser(Jwt jwt) {
        UUID cognitoSub = cognitoUserExtractor.extractSub(jwt);

        AuthUser authUser = authUserRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con cognito_sub: " + cognitoSub));

        List<String> roles = cognitoUserExtractor.extractGroups(jwt);

        return authUserMapper.toAuthUserResponse(authUser, roles);
    }

    @Override
    @Transactional
    public SyncUserResponse syncUser(Jwt jwt) {
        UUID cognitoSub = cognitoUserExtractor.extractSub(jwt);
        String email = cognitoUserExtractor.extractEmail(jwt);

        return authUserRepository.findByCognitoSub(cognitoSub)
                .map(existing -> authUserMapper.toSyncUserResponse(existing, false))
                .orElseGet(() -> createNewUser(cognitoSub, email, jwt.getTokenValue()));
    }

    private SyncUserResponse createNewUser(UUID cognitoSub, String email, String jwt) {
        AuthUser newUser = new AuthUser(
                cognitoSub,
                email,
                DEFAULT_STATUS,
                Instant.now()
        );
        AuthUser saved = authUserRepository.save(newUser);

        String temporaryUsername = buildTemporaryUsername(cognitoSub);
        CreateUserProfileRequest profileRequest = new CreateUserProfileRequest(
                cognitoSub,
                DEFAULT_NAME,
                temporaryUsername,
                null,
                null
        );
        userClient.createUserProfile(profileRequest, jwt);

        return authUserMapper.toSyncUserResponse(saved, true);
    }

    private String buildTemporaryUsername(UUID cognitoSub) {
        return DEFAULT_USERNAME_PREFIX + cognitoSub.toString().substring(0, 8);
    }
}