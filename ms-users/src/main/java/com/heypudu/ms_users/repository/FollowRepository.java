package com.heypudu.ms_users.repository;

import com.heypudu.ms_users.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    Optional<Follow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    @Query("SELECT f.followedId FROM Follow f WHERE f.followerId = :followerId")
    List<UUID> findFollowedIdsByFollowerId(@Param("followerId") UUID followerId);

    void deleteByFollowerId(UUID followerId);

    void deleteByFollowedId(UUID followedId);
}