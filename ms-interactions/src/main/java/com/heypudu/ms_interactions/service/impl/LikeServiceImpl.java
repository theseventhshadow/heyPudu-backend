package com.heypudu.ms_interactions.service.impl;

import com.heypudu.ms_interactions.client.PostClient;
import com.heypudu.ms_interactions.dto.response.LikeCountResponse;
import com.heypudu.ms_interactions.dto.response.LikeResponse;
import com.heypudu.ms_interactions.mapper.InteractionMapper;
import com.heypudu.ms_interactions.model.Like;
import com.heypudu.ms_interactions.repository.LikeRepository;
import com.heypudu.ms_interactions.security.CognitoUserExtractor;
import com.heypudu.ms_interactions.service.LikeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LikeServiceImpl implements LikeService {

    private static final int LIKE_DELTA_UP = 1;
    private static final int LIKE_DELTA_DOWN = -1;
    private static final int NO_COMMENT_DELTA = 0;

    private final LikeRepository likeRepository;
    private final PostClient postClient;
    private final InteractionMapper interactionMapper;
    private final CognitoUserExtractor cognitoUserExtractor;

    public LikeServiceImpl(LikeRepository likeRepository,
                           PostClient postClient,
                           InteractionMapper interactionMapper,
                           CognitoUserExtractor cognitoUserExtractor) {
        this.likeRepository = likeRepository;
        this.postClient = postClient;
        this.interactionMapper = interactionMapper;
        this.cognitoUserExtractor = cognitoUserExtractor;
    }

    @Override
    @Transactional
    public LikeResponse likePost(UUID postId, Jwt jwt) {
        UUID userId = cognitoUserExtractor.extractSub(jwt);

        // Idempotencia: si ya existe, devolver el existente sin notificar.
        Like existing = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);
        if (existing != null) {
            return interactionMapper.toLikeResponse(existing);
        }

        // Validar que el post existe antes de crear el like.
        postClient.validatePostExists(postId, jwt.getTokenValue());

        Like like = new Like(postId, userId, Instant.now());
        Like saved = likeRepository.save(like);

        // Notificar a ms-posts para incrementar like_count.
        postClient.updateCounters(postId, LIKE_DELTA_UP, NO_COMMENT_DELTA, jwt.getTokenValue());

        return interactionMapper.toLikeResponse(saved);
    }

    @Override
    @Transactional
    public void unlikePost(UUID postId, Jwt jwt) {
        UUID userId = cognitoUserExtractor.extractSub(jwt);

        Like existing = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);
        if (existing == null) {
            // Idempotente: si no existe, no hacer nada.
            return;
        }

        likeRepository.delete(existing);

        // Notificar a ms-posts para decrementar like_count.
        postClient.updateCounters(postId, LIKE_DELTA_DOWN, NO_COMMENT_DELTA, jwt.getTokenValue());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LikeResponse> listLikesByPost(UUID postId, Pageable pageable) {
        Page<Like> likes = likeRepository.findByPostId(postId, pageable);
        return interactionMapper.toLikeResponsePage(likes);
    }

    @Override
    @Transactional(readOnly = true)
    public LikeCountResponse countLikesByPost(UUID postId) {
        long count = likeRepository.countByPostId(postId);
        return interactionMapper.toLikeCountResponse(postId, count);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LikeResponse> listMyLikes(Jwt jwt, Pageable pageable) {
        UUID userId = cognitoUserExtractor.extractSub(jwt);
        Page<Like> likes = likeRepository.findByUserId(userId, pageable);
        return interactionMapper.toLikeResponsePage(likes);
    }

    @Override
    @Transactional
    public void deleteAllByPost(UUID postId) {
        likeRepository.deleteByPostId(postId);
    }

    @Override
    @Transactional
    public void deleteAllByUser(UUID userId) {
        likeRepository.deleteByUserId(userId);
    }
}