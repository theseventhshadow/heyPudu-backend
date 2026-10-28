package com.heypudu.ms_auth.mapper;

import com.heypudu.ms_auth.dto.response.AuthUserResponse;
import com.heypudu.ms_auth.dto.response.SyncUserResponse;
import com.heypudu.ms_auth.model.AuthUser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthUserMapper {

    public AuthUserResponse toAuthUserResponse(AuthUser authUser, List<String> roles) {
        return new AuthUserResponse(
                authUser.getId(),
                authUser.getCognitoSub(),
                authUser.getEmail(),
                authUser.getStatus(),
                roles,
                authUser.getCreatedAt()
        );
    }

    public SyncUserResponse toSyncUserResponse(AuthUser authUser, boolean created) {
        return new SyncUserResponse(
                authUser.getId(),
                authUser.getCognitoSub(),
                authUser.getEmail(),
                authUser.getStatus(),
                created,
                authUser.getCreatedAt()
        );
    }
}