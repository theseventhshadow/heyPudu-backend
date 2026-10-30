package com.heypudu.ms_posts.mapper;

import com.heypudu.ms_posts.dto.response.PostResponse;
import com.heypudu.ms_posts.dto.response.PostSummaryResponse;
import com.heypudu.ms_posts.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    private static final int CONTENT_PREVIEW_LENGTH = 200;

    private static String previewOf(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.strip();
        if (trimmed.length() <= CONTENT_PREVIEW_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, CONTENT_PREVIEW_LENGTH).stripTrailing() + "...";
    }

    public PostResponse toPostResponse(Post post, String audioUrl, String coverImageUrl) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getAuthorUsername(),
                post.getAuthorName(),
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

    public PostSummaryResponse toPostSummaryResponse(Post post, String coverImageUrl, String audioUrl) {
        return new PostSummaryResponse(
                post.getId(),
                post.getAuthorId(),
                post.getAuthorUsername(),
                post.getAuthorName(),
                post.getAuthorAvatarUrl(),
                post.getType(),
                post.getTitle(),
                previewOf(post.getContent()),
                coverImageUrl,
                audioUrl,
                post.getAudioDurationSeconds(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt()
        );
    }

    public Page<PostSummaryResponse> toPostSummaryResponsePage(
            Page<Post> posts,
            java.util.function.Function<Post, String> coverUrlResolver,
            java.util.function.Function<Post, String> audioUrlResolver) {
        return posts.map(post -> toPostSummaryResponse(
                post, coverUrlResolver.apply(post), audioUrlResolver.apply(post)));
    }
}