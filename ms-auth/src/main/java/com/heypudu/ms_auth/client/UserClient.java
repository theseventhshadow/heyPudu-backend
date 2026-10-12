package com.heypudu.ms_auth.client;

import com.heypudu.ms_auth.dto.request.CreateUserProfileRequest;
import com.heypudu.ms_auth.dto.response.UserSummaryResponse;
import com.heypudu.ms_auth.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient usersRestClient;

    public UserClient(@Qualifier("usersRestClient") RestClient usersRestClient) {
        this.usersRestClient = usersRestClient;
    }

    public UserSummaryResponse createUserProfile(CreateUserProfileRequest request, String jwt) {
        try {
            return usersRestClient.post()
                    .uri("/api/v1/internal/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .body(request)
                    .retrieve()
                    .body(UserSummaryResponse.class);
        } catch (RestClientException ex) {
            log.error("No se pudo crear el perfil del usuario {} en ms-users: {}",
                    request.cognitoSub(), ex.getMessage());
            throw new BusinessException(
                    "No se pudo crear el perfil del usuario en ms-users: " + request.cognitoSub());
        }
    }
}