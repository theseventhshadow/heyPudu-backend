package com.heypudu.ms_posts.repository;

import com.heypudu.ms_posts.model.PostReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostReportRepository extends JpaRepository<PostReport, UUID> {

    boolean existsByPostIdAndReporterId(UUID postId, UUID reporterId);

    Page<PostReport> findByStatus(String status, Pageable pageable);

    void deleteByPostId(UUID postId);
}
