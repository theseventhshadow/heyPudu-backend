package com.heypudu.ms_interactions.service;

import com.heypudu.ms_interactions.dto.request.CreateCommentRequest;
import com.heypudu.ms_interactions.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface CommentService {

    /**
     * Crea un comentario en un post.
     * Valida que el post existe en ms-posts.
     * Notifica a ms-posts para incrementar comment_count.
     */
    CommentResponse createComment(UUID postId, CreateCommentRequest request, Jwt jwt);

    /**
     * Lista los comentarios de un post, paginados.
     */
    Page<CommentResponse> listCommentsByPost(UUID postId, Pageable pageable);

    /**
     * Elimina un comentario propio.
     * Solo el autor puede eliminar su comentario.
     * Notifica a ms-posts para decrementar comment_count.
     */
    void deleteComment(UUID commentId, Jwt jwt);

    /**
     * Elimina un comentario como admin.
     * Un admin puede eliminar cualquier comentario.
     * Notifica a ms-posts para decrementar comment_count.
     */
    void deleteCommentAsAdmin(UUID commentId);

    /**
     * Elimina todos los comentarios de un post.
     * Uso interno: llamado cuando ms-posts elimina un post.
     */
    void deleteAllByPost(UUID postId);

    /**
     * Elimina todos los comentarios de un usuario.
     * Uso interno: llamado cuando ms-users anonimiza un usuario.
     */
    void deleteAllByUser(UUID userId);
}