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
}