package com.heypudu.ms_interactions.service.impl;

import com.heypudu.ms_interactions.client.PostClient;
import com.heypudu.ms_interactions.dto.request.CreateCommentRequest;
import com.heypudu.ms_interactions.dto.response.CommentResponse;
import com.heypudu.ms_interactions.exception.BusinessException;
import com.heypudu.ms_interactions.exception.ResourceNotFoundException;
import com.heypudu.ms_interactions.mapper.InteractionMapper;
import com.heypudu.ms_interactions.model.Comment;
import com.heypudu.ms_interactions.repository.CommentRepository;
import com.heypudu.ms_interactions.security.CognitoUserExtractor;
import com.heypudu.ms_interactions.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CommentServiceImpl implements CommentService {

    private static final int COMMENT_DELTA_UP = 1;
    private static final int COMMENT_DELTA_DOWN = -1;
    private static final int NO_LIKE_DELTA = 0;

    private final CommentRepository commentRepository;
    private final PostClient postClient;
    private final InteractionMapper interactionMapper;
    private final CognitoUserExtractor cognitoUserExtractor;

    public CommentServiceImpl(CommentRepository commentRepository,
                              PostClient postClient,
                              InteractionMapper interactionMapper,
                              CognitoUserExtractor cognitoUserExtractor) {
        this.commentRepository = commentRepository;
        this.postClient = postClient;
        this.interactionMapper = interactionMapper;
        this.cognitoUserExtractor = cognitoUserExtractor;
    }

    @Override
    @Transactional
    public CommentResponse createComment(UUID postId, CreateCommentRequest request, Jwt jwt) {
        UUID userId = cognitoUserExtractor.extractSub(jwt);

        // Validar que el post existe antes de crear el comentario.
        postClient.validatePostExists(postId, jwt.getTokenValue());

        Comment comment = new Comment(postId, userId, request.content(), Instant.now());
        Comment saved = commentRepository.save(comment);

        // Notificar a ms-posts para incrementar comment_count.
        postClient.updateCounters(postId, NO_LIKE_DELTA, COMMENT_DELTA_UP, jwt.getTokenValue());

        return interactionMapper.toCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> listCommentsByPost(UUID postId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByPostId(postId, pageable);
        return interactionMapper.toCommentResponsePage(comments);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, Jwt jwt) {
        UUID userId = cognitoUserExtractor.extractSub(jwt);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comentario no encontrado con id: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("No puedes eliminar un comentario que no es tuyo");
        }

        commentRepository.delete(comment);

        // Notificar a ms-posts para decrementar comment_count.
        postClient.updateCounters(
                comment.getPostId(),
                NO_LIKE_DELTA,
                COMMENT_DELTA_DOWN,
                jwt.getTokenValue()
        );
    }

    @Override
    @Transactional
    public void deleteCommentAsAdmin(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comentario no encontrado con id: " + commentId));

        commentRepository.delete(comment);

        // Notificar a ms-posts para decrementar comment_count.
        postClient.updateCounters(
                comment.getPostId(),
                NO_LIKE_DELTA,
                COMMENT_DELTA_DOWN,
                null
        );
    }

    @Override
    @Transactional
    public void deleteAllByPost(UUID postId) {
        commentRepository.deleteByPostId(postId);
    }

    @Override
    @Transactional
    public void deleteAllByUser(UUID userId) {
        commentRepository.deleteByUserId(userId);
    }
}