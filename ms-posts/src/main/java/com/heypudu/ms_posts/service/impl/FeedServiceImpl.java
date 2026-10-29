package com.heypudu.ms_posts.service.impl;

import com.heypudu.ms_posts.client.UserClient;
import com.heypudu.ms_posts.dto.response.PostSummaryResponse;
import com.heypudu.ms_posts.mapper.PostMapper;
import com.heypudu.ms_posts.model.Post;
import com.heypudu.ms_posts.repository.PostRepository;
import com.heypudu.ms_posts.security.CognitoUserExtractor;
import com.heypudu.ms_posts.service.FeedService;
import com.heypudu.ms_posts.service.S3Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FeedServiceImpl implements FeedService {

    private final PostRepository postRepository;
    private final UserClient userClient;
    private final S3Service s3Service;
    private final PostMapper postMapper;
    private final CognitoUserExtractor cognitoUserExtractor;

    public FeedServiceImpl(PostRepository postRepository,
                           UserClient userClient,
                           S3Service s3Service,
                           PostMapper postMapper,
                           CognitoUserExtractor cognitoUserExtractor) {
        this.postRepository = postRepository;
        this.userClient = userClient;
        this.s3Service = s3Service;
        this.postMapper = postMapper;
        this.cognitoUserExtractor = cognitoUserExtractor;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getGeneralFeed(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);
        return toSummaryPage(posts);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPersonalFeed(Jwt jwt, Pageable pageable) {
        UUID userId = cognitoUserExtractor.extractSub(jwt);

        List<UUID> authorIds = new ArrayList<>();
        authorIds.add(userId);
        authorIds.addAll(userClient.getFollowingIds(userId, jwt.getTokenValue()));

        Page<Post> posts = postRepository.findByAuthorIdIn(authorIds, pageable);
        return toSummaryPage(posts);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> searchPosts(String query, Pageable pageable) {
        Page<Post> posts = postRepository.searchByTitleOrContent(query, pageable);
        return toSummaryPage(posts);
    }

    private Page<PostSummaryResponse> toSummaryPage(Page<Post> posts) {
        return postMapper.toPostSummaryResponsePage(
                posts, this::resolveCoverUrl, this::resolveAudioUrl);
    }

    private String resolveAudioUrl(Post post) {
        return post.getAudioKey() != null
                ? s3Service.generateAudioDownloadUrl(post.getAudioKey())
                : null;
    }

    private String resolveCoverUrl(Post post) {
        return post.getCoverImageKey() != null
                ? s3Service.generateCoverDownloadUrl(post.getCoverImageKey())
                : null;
    }
}