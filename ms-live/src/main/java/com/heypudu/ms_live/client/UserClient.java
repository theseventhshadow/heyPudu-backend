package com.heypudu.ms_live.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class UserClient {

    private final RestClient usersRestClient;

    public UserClient(RestClient usersRestClient) {
        this.usersRestClient = usersRestClient;
    }

    public UserSummaryDto getSummaryByCognitoSub(UUID cognitoSub) {
        return usersRestClient.get()
                .uri("/api/v1/internal/users/by-cognito/{cognitoSub}/summary", cognitoSub)
                .retrieve()
                .body(UserSummaryDto.class);
    }

    public boolean isFollowing(UUID creatorSub, UUID listenerSub, String bearerToken) {
        try {
            Boolean response = usersRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/internal/users/by-cognito/{creatorSub}/followers/check")
                            .queryParam("followerCognitoSub", listenerSub)
                            .build(creatorSub))
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .body(Boolean.class);
            return Boolean.TRUE.equals(response);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            throw ex;
        } catch (RestClientException ex) {
            throw ex;
        }
    }
}
