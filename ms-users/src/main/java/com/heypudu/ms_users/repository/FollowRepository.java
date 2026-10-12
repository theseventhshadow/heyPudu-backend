package com.heypudu.ms_users.repository;

import com.heypudu.ms_users.model.Follow;
import com.heypudu.ms_users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(
            value = """
                    SELECT u FROM User u
                    JOIN Follow f ON f.followerId = u.id
                    WHERE f.followedId = :userId
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM Follow f
                    WHERE f.followedId = :userId
                    """
    )
    Page<User> findFollowersAsUsers(@Param("userId") UUID userId, Pageable pageable);

    @Query(
            value = """
                    SELECT u FROM User u
                    JOIN Follow f ON f.followedId = u.id
                    WHERE f.followerId = :userId
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM Follow f
                    WHERE f.followerId = :userId
                    """
    )
    Page<User> findFollowingAsUsers(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT f.followedId FROM Follow f WHERE f.followerId = :followerId")
    List<UUID> findFollowedIdsByFollowerId(@Param("followerId") UUID followerId);

    void deleteByFollowerId(UUID followerId);

    void deleteByFollowedId(UUID followedId);
}