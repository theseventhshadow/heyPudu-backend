package com.heypudu.ms_users.repository;

import com.heypudu.ms_users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByCognitoSub(UUID cognitoSub);

    boolean existsByCognitoSub(UUID cognitoSub);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("""
            SELECT u FROM User u
            WHERE u.status = 'ACTIVE'
              AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<User> searchByNameOrUsername(@Param("q") String query, Pageable pageable);

    @Query(
            value = """
                    SELECT u FROM User u
                    WHERE u.id IN (
                        SELECT f.followerId FROM Follow f WHERE f.followedId = :userId
                    )
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM Follow f
                    WHERE f.followedId = :userId
                    """
    )
    Page<User> findFollowersOf(@Param("userId") UUID userId, Pageable pageable);

    @Query(
            value = """
                    SELECT u FROM User u
                    WHERE u.id IN (
                        SELECT f.followedId FROM Follow f WHERE f.followerId = :userId
                    )
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM Follow f
                    WHERE f.followerId = :userId
                    """
    )
    Page<User> findFollowingOf(@Param("userId") UUID userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.followerCount = u.followerCount + :delta,
                u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.id = :userId
            """)
    int updateFollowerCount(@Param("userId") UUID userId, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.followingCount = u.followingCount + :delta,
                u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.id = :userId
            """)
    int updateFollowingCount(@Param("userId") UUID userId, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.postCount = CASE WHEN u.postCount + :delta < 0 THEN 0 ELSE u.postCount + :delta END,
                u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.id = :userId
            """)
    int updatePostCount(@Param("userId") UUID userId, @Param("delta") int delta);
}