package com.heypudu.ms_interactions.mapper;

import com.heypudu.ms_interactions.dto.response.CommentResponse;
import com.heypudu.ms_interactions.dto.response.LikeCountResponse;
import com.heypudu.ms_interactions.dto.response.LikeResponse;
import com.heypudu.ms_interactions.model.Comment;
import com.heypudu.ms_interactions.model.Like;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InteractionMapper {

    public LikeResponse toLikeResponse(Like like) {
        return new LikeResponse(
                like.getId(),
                like.getPostId(),
                like.getUserId(),
                like.getCreatedAt()
        );
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public LikeCountResponse toLikeCountResponse(UUID postId, long count) {
        return new LikeCountResponse(postId, count);
    }

    public Page<LikeResponse> toLikeResponsePage(Page<Like> likes) {
        return likes.map(this::toLikeResponse);
    }

    public Page<CommentResponse> toCommentResponsePage(Page<Comment> comments) {
        return comments.map(this::toCommentResponse);
    }
}