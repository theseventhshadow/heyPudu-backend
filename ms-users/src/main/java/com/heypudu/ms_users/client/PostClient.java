package com.heypudu.ms_users.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de comunicacion con ms-posts.
 *
 * NO se utiliza en la EP1. Se activa solo cuando la propiedad
 * clients.posts.enabled = true. Por defecto esta deshabilitado.
 *
 * Uso futuro previsto:
 *   - Consultar el post_count real de un usuario.
 *   - Notificar a ms-posts sobre cambios en el perfil del autor
 *     (por ejemplo, cambio de username o avatar).
 */
@Component
@ConditionalOnProperty(name = "clients.posts.enabled", havingValue = "true", matchIfMissing = false)
public class PostClient {

    private static final Logger log = LoggerFactory.getLogger(PostClient.class);

    private final RestClient postsRestClient;

    public PostClient(@Qualifier("postsRestClient") RestClient postsRestClient) {
        this.postsRestClient = postsRestClient;
    }

    // TODO EP2: implementar metodos cuando se necesite la comunicacion real.
    //
    // public long getPostCount(UUID authorId, String jwt) {
    //     try {
    //         return postsRestClient.get()
    //                 .uri("/api/v1/internal/posts/users/{authorId}/count", authorId)
    //                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
    //                 .retrieve()
    //                 .body(Long.class);
    //     } catch (RestClientException ex) {
    //         log.warn("No se pudo obtener el post_count del usuario {}: {}",
    //                 authorId, ex.getMessage());
    //         return 0L;
    //     }
    // }
}