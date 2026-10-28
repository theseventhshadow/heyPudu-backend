package com.heypudu.ms_users.client;

import com.heypudu.ms_users.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final RestClient authRestClient;

    public AuthClient(@Qualifier("authRestClient") RestClient authRestClient) {
        this.authRestClient = authRestClient;
    }

    public void disableUser(UUID cognitoSub, String jwt) {
        try {
            authRestClient.put()
                    .uri("/api/v1/internal/users/{cognitoSub}/disable", cognitoSub)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("No se pudo deshabilitar el usuario {} en Cognito: {}",
                    cognitoSub, ex.getMessage());
            throw new BusinessException(
                    "No se pudo deshabilitar el usuario en Cognito: " + cognitoSub);
        }
    }

    public void enableUser(UUID cognitoSub, String jwt) {
        try {
            authRestClient.put()
                    .uri("/api/v1/internal/users/{cognitoSub}/enable", cognitoSub)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("No se pudo habilitar el usuario {} en Cognito: {}",
                    cognitoSub, ex.getMessage());
            throw new BusinessException(
                    "No se pudo habilitar el usuario en Cognito: " + cognitoSub);
        }
    }

    public void deleteUser(UUID cognitoSub, String jwt) {
        try {
            authRestClient.delete()
                    .uri("/api/v1/internal/users/{cognitoSub}", cognitoSub)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("No se pudo eliminar el usuario {} en Cognito: {}",
                    cognitoSub, ex.getMessage());
            throw new BusinessException(
                    "No se pudo eliminar el usuario en Cognito: " + cognitoSub);
        }
    }
}