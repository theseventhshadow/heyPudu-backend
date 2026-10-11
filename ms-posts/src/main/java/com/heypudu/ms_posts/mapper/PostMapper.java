package com.heypudu.ms_posts.mapper;

import com.heypudu.ms_posts.dto.response.PostResponse;
import com.heypudu.ms_posts.dto.response.PostSummaryResponse;
import com.heypudu.ms_posts.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toPostResponse(Post post, String audioUrl, String coverImageUrl) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getAuthorUsername(),
                post.getAuthorAvatarUrl(),
                post.getType(),
                post.getTitle(),
                post.getContent(),
                audioUrl,
                post.getAudioDurationSeconds(),
                coverImageUrl,
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public PostSummaryResponse toPostSummaryResponse(Post post, String coverImageUrl) {
        return new PostSummaryResponse(
                post.getId(),
                post.getAuthorId(),
                post.getAuthorUsername(),
                post.getAuthorAvatarUrl(),
                post.getType(),
                post.getTitle(),
                coverImageUrl,
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt()
        );
    }

    public Page<PostSummaryResponse> toPostSummaryResponsePage(Page<Post> posts, java.util.function.Function<Post, String> coverUrlResolver) {
        return posts.map(post -> toPostSummaryResponse(post, coverUrlResolver.apply(post)));
    }
}