package com.heypudu.ms_users.repository;

import com.heypudu.ms_users.model.Notification;
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
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventId(UUID eventId);

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
            UUID recipientId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.read = true
            WHERE n.recipientId = :recipientId
              AND n.read = false
            """)
    int markAllAsRead(@Param("recipientId") UUID recipientId);
}
