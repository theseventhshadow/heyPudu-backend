package com.heypudu.ms_interactions.service;

import com.heypudu.ms_interactions.dto.response.LikeCountResponse;
import com.heypudu.ms_interactions.dto.response.LikeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface LikeService {

    /**
     * Da like a un post.
     * Valida que el post existe en ms-posts.
     * Si el usuario ya habia dado like, no hace nada (idempotente).
     * Notifica a ms-posts para incrementar like_count.
     */
    LikeResponse likePost(UUID postId, Jwt jwt);

    /**
     * Quita el like de un post.
     * Si el usuario no habia dado like, no hace nada (idempotente).
     * Notifica a ms-posts para decrementar like_count.
     */
    void unlikePost(UUID postId, Jwt jwt);

    /**
     * Lista los likes de un post, paginados.
     */
    Page<LikeResponse> listLikesByPost(UUID postId, Pageable pageable);

    /**
     * Cuenta los likes de un post.
     */
    LikeCountResponse countLikesByPost(UUID postId);

    /**
     * Lista los posts a los que el usuario autenticado dio like, paginados.
     */
    Page<LikeResponse> listMyLikes(Jwt jwt, Pageable pageable);

    /**
     * Elimina todos los likes de un post.
     * Uso interno: llamado cuando ms-posts elimina un post.
     */
    void deleteAllByPost(UUID postId);

    /**
     * Elimina todos los likes de un usuario.
     * Uso interno: llamado cuando ms-users anonimiza un usuario.
     */
    void deleteAllByUser(UUID userId);
}