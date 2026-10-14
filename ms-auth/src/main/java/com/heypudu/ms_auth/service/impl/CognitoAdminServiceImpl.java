package com.heypudu.ms_auth.service.impl;

import com.heypudu.ms_auth.exception.BusinessException;
import com.heypudu.ms_auth.service.CognitoAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDisableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminEnableUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import java.util.UUID;

@Service
public class CognitoAdminServiceImpl implements CognitoAdminService {

    private static final Logger log = LoggerFactory.getLogger(CognitoAdminServiceImpl.class);

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    public CognitoAdminServiceImpl(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    @Override
    public void disableUser(UUID cognitoSub) {
        try {
            AdminDisableUserRequest request = AdminDisableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(cognitoSub.toString())
                    .build();
            cognitoClient.adminDisableUser(request);
        } catch (CognitoIdentityProviderException ex) {
            log.error("No se pudo deshabilitar el usuario {} en Cognito: {}",
                    cognitoSub, ex.getMessage());
            throw new BusinessException(
                    "No se pudo deshabilitar el usuario en Cognito: " + cognitoSub);
        }
    }

    @Override
    public void enableUser(UUID cognitoSub) {
        try {
            AdminEnableUserRequest request = AdminEnableUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(cognitoSub.toString())
                    .build();
            cognitoClient.adminEnableUser(request);
        } catch (CognitoIdentityProviderException ex) {
            log.error("No se pudo habilitar el usuario {} en Cognito: {}",
                    cognitoSub, ex.getMessage());
            throw new BusinessException(
                    "No se pudo habilitar el usuario en Cognito: " + cognitoSub);
        }
    }

    @Override
    public void deleteUser(UUID cognitoSub) {
        try {
            AdminDeleteUserRequest request = AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(cognitoSub.toString())
                    .build();
            cognitoClient.adminDeleteUser(request);
        } catch (CognitoIdentityProviderException ex) {
            log.error("No se pudo eliminar el usuario {} en Cognito: {}",
                    cognitoSub, ex.getMessage());
            throw new BusinessException(
                    "No se pudo eliminar el usuario en Cognito: " + cognitoSub);
        }
    }
}