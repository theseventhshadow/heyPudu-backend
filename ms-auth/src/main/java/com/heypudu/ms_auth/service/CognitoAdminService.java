package com.heypudu.ms_auth.service;

import java.util.UUID;

public interface CognitoAdminService {

    void disableUser(UUID cognitoSub);

    void enableUser(UUID cognitoSub);

    void deleteUser(UUID cognitoSub);
}