package com.heypudu.ms_interactions.repository;

import com.heypudu.ms_interactions.model.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    Optional<Like> findByPostIdAndUserId(UUID postId, UUID userId);

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Page<Like> findByPostId(UUID postId, Pageable pageable);

    Page<Like> findByUserId(UUID userId, Pageable pageable);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);

    void deleteByUserId(UUID userId);
}