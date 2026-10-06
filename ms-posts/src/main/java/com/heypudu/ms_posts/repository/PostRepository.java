package com.heypudu.ms_posts.repository;

import com.heypudu.ms_posts.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);

    Page<Post> findByAuthorIdIn(List<UUID> authorIds, Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Post> searchByTitleOrContent(@Param("q") String query, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Post p
            SET p.likeCount = p.likeCount + :likeDelta,
                p.commentCount = p.commentCount + :commentDelta,
                p.updatedAt = CURRENT_TIMESTAMP
            WHERE p.id = :postId
            """)
    int updateCounters(@Param("postId") UUID postId,
                       @Param("likeDelta") int likeDelta,
                       @Param("commentDelta") int commentDelta);

    @Modifying
    @Query("""
            UPDATE Post p
            SET p.authorUsername = :anonymousUsername,
                p.authorAvatarUrl = NULL,
                p.updatedAt = CURRENT_TIMESTAMP
            WHERE p.authorId = :authorId
            """)
    int anonymizeByAuthorId(@Param("authorId") UUID authorId,
                            @Param("anonymousUsername") String anonymousUsername);
}