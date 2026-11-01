package com.heypudu.ms_users.service.impl;

import com.heypudu.ms_users.config.amqp.RabbitTopology;
import com.heypudu.ms_users.dto.response.NotificationResponse;
import com.heypudu.ms_users.exception.ResourceNotFoundException;
import com.heypudu.ms_users.messaging.NotificationEvent;
import com.heypudu.ms_users.model.Notification;
import com.heypudu.ms_users.repository.NotificationRepository;
import com.heypudu.ms_users.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final String TYPE_LIKE = "LIKE";
    private static final String TYPE_COMMENT = "COMMENT";
    private static final String TYPE_FOLLOW = "FOLLOW";

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void createFromEvent(NotificationEvent event, String routingKey) {
        validateEvent(event);
        if (notificationRepository.existsByEventId(event.eventId())) {
            return;
        }

        String type = resolveType(routingKey);
        Notification notification = new Notification(
                event.eventId(),
                event.recipientId(),
                event.actorId(),
                type,
                event.postId(),
                event.commentId(),
                messageFor(type),
                event.occurredAt() != null ? event.occurredAt() : Instant.now()
        );
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(UUID recipientId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
                recipientId, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
        return notifications.map(this::toResponse);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID recipientId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificacion no encontrada: " + notificationId));
        notification.markAsRead();
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID recipientId) {
        notificationRepository.markAllAsRead(recipientId);
    }

    private void validateEvent(NotificationEvent event) {
        if (event == null
                || event.eventId() == null
                || event.actorId() == null
                || event.recipientId() == null) {
            throw new IllegalArgumentException("El evento de notificacion no tiene identificadores validos");
        }
    }

    private String resolveType(String routingKey) {
        return switch (routingKey) {
            case RabbitTopology.RK_INTERACTION_LIKE -> TYPE_LIKE;
            case RabbitTopology.RK_INTERACTION_COMMENT -> TYPE_COMMENT;
            case RabbitTopology.RK_USER_FOLLOWED -> TYPE_FOLLOW;
            default -> throw new IllegalArgumentException(
                    "Routing key de notificacion no soportada: " + routingKey);
        };
    }

    private String messageFor(String type) {
        return switch (type) {
            case TYPE_LIKE -> "Tu publicacion recibio un like";
            case TYPE_COMMENT -> "Tu publicacion recibio un comentario";
            case TYPE_FOLLOW -> "Tienes un nuevo seguidor";
            default -> throw new IllegalArgumentException("Tipo de notificacion no soportado: " + type);
        };
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getActorId(),
                notification.getType(),
                notification.getPostId(),
                notification.getCommentId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
