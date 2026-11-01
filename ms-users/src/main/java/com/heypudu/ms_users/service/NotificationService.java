package com.heypudu.ms_users.service;

import com.heypudu.ms_users.dto.response.NotificationResponse;
import com.heypudu.ms_users.messaging.NotificationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    void createFromEvent(NotificationEvent event, String routingKey);

    Page<NotificationResponse> list(UUID recipientId, boolean unreadOnly, Pageable pageable);

    void markAsRead(UUID notificationId, UUID recipientId);

    void markAllAsRead(UUID recipientId);
}
