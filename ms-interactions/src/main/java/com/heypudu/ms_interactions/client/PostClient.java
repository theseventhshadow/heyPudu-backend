package com.heypudu.ms_interactions.client;

import com.heypudu.ms_interactions.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class PostClient {

    private static final Logger log = LoggerFactory.getLogger(PostClient.class);

    private final RestClient postsRestClient;

    public PostClient(RestClient postsRestClient) {
        this.postsRestClient = postsRestClient;
    }

    /**
     * Verifica que un post exista en ms-posts.
     * Lanza BusinessException si no existe o si ms-posts no responde.
     */
    public void validatePostExists(UUID postId, String jwt) {
        try {
            postsRestClient.get()
                    .uri("/api/v1/posts/{id}", postId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("No se pudo validar el post {}: {}", postId, ex.getMessage());
            throw new BusinessException(
                    "No se pudo validar la existencia del post con id: " + postId);
        }
    }

    /**
     * Notifica a ms-posts para ajustar los contadores de un post.
     * likeDelta y commentDelta pueden ser positivos o negativos.
     */
    public void updateCounters(UUID postId, int likeDelta, int commentDelta, String jwt) {
        CounterUpdateRequest request = new CounterUpdateRequest(likeDelta, commentDelta);

        try {
            postsRestClient.patch()
                    .uri("/api/v1/internal/posts/{id}/counters", postId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("No se pudieron actualizar los contadores del post {}: {}",
                    postId, ex.getMessage());
            throw new BusinessException(
                    "No se pudieron actualizar los contadores del post con id: " + postId);
        }
    }

    /**
     * DTO interno para el body del PATCH de contadores.
     * Es privado porque no se expone fuera del cliente.
     */
    private record CounterUpdateRequest(int likeDelta, int commentDelta) {
    }
}