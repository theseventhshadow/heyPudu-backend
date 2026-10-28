package com.heypudu.ms_posts.client;

import com.heypudu.ms_posts.dto.response.UserSummaryResponse;
import com.heypudu.ms_posts.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient usersRestClient;

    public UserClient(@Qualifier("usersRestClient") RestClient usersRestClient) {
        this.usersRestClient = usersRestClient;
    }

    public UserSummaryResponse getUser(UUID userId, String jwt) {
        try {
            return usersRestClient.get()
                    .uri("/api/v1/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .body(UserSummaryResponse.class);
        } catch (RestClientException ex) {
            log.warn("No se pudo obtener el usuario {}: {}", userId, ex.getMessage());
            throw new BusinessException(
                    "No se pudo obtener el usuario con id: " + userId);
        }
    }

    public List<UUID> getFollowingIds(UUID userId, String jwt) {
        try {
            UUID[] following = usersRestClient.get()
                    .uri("/api/v1/users/{id}/following/ids", userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .body(UUID[].class);

            return following != null ? Arrays.asList(following) : List.of();
        } catch (RestClientException ex) {
            log.warn("No se pudo obtener la lista de seguidos del usuario {}: {}",
                    userId, ex.getMessage());
            throw new BusinessException(
                    "No se pudo obtener la lista de seguidos del usuario con id: " + userId);
        }
    }
}