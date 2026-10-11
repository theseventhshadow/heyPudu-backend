package com.heypudu.ms_posts.service.impl;

import com.heypudu.ms_posts.client.InteractionClient;
import com.heypudu.ms_posts.client.UserClient;
import com.heypudu.ms_posts.dto.request.CreatePostRequest;
import com.heypudu.ms_posts.dto.request.PresignedUrlRequest;
import com.heypudu.ms_posts.dto.request.UpdatePostRequest;
import com.heypudu.ms_posts.dto.response.PostResponse;
import com.heypudu.ms_posts.dto.response.PresignedUrlResponse;
import com.heypudu.ms_posts.dto.response.UserSummaryResponse;
import com.heypudu.ms_posts.exception.BusinessException;
import com.heypudu.ms_posts.exception.ResourceNotFoundException;
import com.heypudu.ms_posts.mapper.PostMapper;
import com.heypudu.ms_posts.model.Post;
import com.heypudu.ms_posts.repository.PostRepository;
import com.heypudu.ms_posts.security.CognitoUserExtractor;
import com.heypudu.ms_posts.service.PostService;
import com.heypudu.ms_posts.service.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PostServiceImpl implements PostService {

    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_AUDIO = "AUDIO";
    private static final String ANONYMOUS_USERNAME = "Usuario eliminado";

    private final PostRepository postRepository;
    private final UserClient userClient;
    private final InteractionClient interactionClient;
    private final S3Service s3Service;
    private final PostMapper postMapper;
    private final CognitoUserExtractor cognitoUserExtractor;

    public PostServiceImpl(PostRepository postRepository,
                           UserClient userClient,
                           InteractionClient interactionClient,
                           S3Service s3Service,
                           PostMapper postMapper,
                           CognitoUserExtractor cognitoUserExtractor) {
        this.postRepository = postRepository;
        this.userClient = userClient;
        this.interactionClient = interactionClient;
        this.s3Service = s3Service;
        this.postMapper = postMapper;
        this.cognitoUserExtractor = cognitoUserExtractor;
    }

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, Jwt jwt) {
        UUID authorId = cognitoUserExtractor.extractSub(jwt);

        validateCreateRequest(request);

        UserSummaryResponse author = userClient.getUser(authorId, jwt.getTokenValue());

        Post post = new Post(
                authorId,
                author.username(),
                author.avatarUrl(),
                request.type(),
                request.title(),
                request.content(),
                request.audioKey(),
                request.audioDurationSeconds(),
                request.coverImageKey(),
                Instant.now()
        );

        Post saved = postRepository.save(post);
        return toPostResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId, Jwt jwt) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Publicacion no encontrada con id: " + postId));
        return toPostResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getMyPosts(Jwt jwt, Pageable pageable) {
        UUID authorId = cognitoUserExtractor.extractSub(jwt);
        Page<Post> posts = postRepository.findByAuthorId(authorId, pageable);
        return posts.map(this::toPostResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByAuthor(UUID authorId, Pageable pageable) {
        Page<Post> posts = postRepository.findByAuthorId(authorId, pageable);
        return posts.map(this::toPostResponse);
    }

    @Override
    @Transactional
    public PostResponse updatePost(UUID postId, UpdatePostRequest request, Jwt jwt) {
        UUID userId = cognitoUserExtractor.extractSub(jwt);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Publicacion no encontrada con id: " + postId));

        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException("No puedes editar una publicacion que no es tuya");
        }

        post.setTitle(request.title());
        post.setCoverImageKey(request.coverImageKey());
        post.setUpdatedAt(Instant.now());

        if (TYPE_TEXT.equals(post.getType())) {
            post.setContent(request.content());
        } else if (request.content() != null && !request.content().isBlank()) {
            throw new BusinessException("No se puede asignar contenido de texto a una publicacion de audio");
        }

        Post saved = postRepository.save(post);
        return toPostResponse(saved);
    }

    @Override
    @Transactional
    public void deletePost(UUID postId, Jwt jwt) {
        UUID userId = cognitoUserExtractor.extractSub(jwt);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Publicacion no encontrada con id: " + postId));

        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException("No puedes eliminar una publicacion que no es tuya");
        }

        interactionClient.deleteInteractionsByPost(postId, jwt.getTokenValue());

        if (post.getAudioKey() != null) {
            s3Service.deleteAudio(post.getAudioKey());
        }
        if (post.getCoverImageKey() != null) {
            s3Service.deleteCover(post.getCoverImageKey());
        }

        postRepository.delete(post);
    }

    @Override
    public PresignedUrlResponse generateAudioUploadUrl(PresignedUrlRequest request, Jwt jwt) {
        return s3Service.generateAudioUploadUrl(request);
    }

    @Override
    public PresignedUrlResponse generateCoverUploadUrl(PresignedUrlRequest request, Jwt jwt) {
        return s3Service.generateCoverUploadUrl(request);
    }

    @Override
    @Transactional
    public void updateCounters(UUID postId, int likeDelta, int commentDelta) {
        int updated = postRepository.updateCounters(postId, likeDelta, commentDelta);
        if (updated == 0) {
            throw new ResourceNotFoundException(
                    "Publicacion no encontrada con id: " + postId);
        }
    }

    @Override
    @Transactional
    public void anonymizeByAuthorId(UUID authorId) {
        postRepository.anonymizeByAuthorId(authorId, ANONYMOUS_USERNAME);
    }

    private void validateCreateRequest(CreatePostRequest request) {
        if (TYPE_TEXT.equals(request.type())) {
            if (request.content() == null || request.content().isBlank()) {
                throw new BusinessException("El contenido es obligatorio para publicaciones de tipo TEXT");
            }
            if (request.audioKey() != null || request.audioDurationSeconds() != null) {
                throw new BusinessException("No se puede enviar audio en una publicacion de tipo TEXT");
            }
        } else if (TYPE_AUDIO.equals(request.type())) {
            if (request.audioKey() == null || request.audioKey().isBlank()) {
                throw new BusinessException("El audio es obligatorio para publicaciones de tipo AUDIO");
            }
            if (request.audioDurationSeconds() == null) {
                throw new BusinessException("La duracion del audio es obligatoria para publicaciones de tipo AUDIO");
            }
            if (request.content() != null && !request.content().isBlank()) {
                throw new BusinessException("No se puede enviar contenido de texto en una publicacion de tipo AUDIO");
            }
        }
    }

    private PostResponse toPostResponse(Post post) {
        String audioUrl = post.getAudioKey() != null
                ? s3Service.generateAudioDownloadUrl(post.getAudioKey())
                : null;
        String coverUrl = post.getCoverImageKey() != null
                ? s3Service.generateCoverDownloadUrl(post.getCoverImageKey())
                : null;
        return postMapper.toPostResponse(post, audioUrl, coverUrl);
    }
}