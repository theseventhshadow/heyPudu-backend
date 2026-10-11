package com.heypudu.ms_posts.client;

import com.heypudu.ms_posts.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class InteractionClient {

    private static final Logger log = LoggerFactory.getLogger(InteractionClient.class);

    private final RestClient interactionsRestClient;

    public InteractionClient(@Qualifier("interactionsRestClient") RestClient interactionsRestClient) {
        this.interactionsRestClient = interactionsRestClient;
    }

    public void deleteInteractionsByPost(UUID postId, String jwt) {
        try {
            interactionsRestClient.delete()
                    .uri("/api/v1/internal/posts/{postId}/interactions", postId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("No se pudieron eliminar las interacciones del post {}: {}",
                    postId, ex.getMessage());
            throw new BusinessException(
                    "No se pudieron eliminar las interacciones del post con id: " + postId);
        }
    }
}