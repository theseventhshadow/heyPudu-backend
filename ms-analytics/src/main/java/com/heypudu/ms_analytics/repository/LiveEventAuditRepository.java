package com.heypudu.ms_analytics.repository;

import com.heypudu.ms_analytics.model.LiveEventAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LiveEventAuditRepository extends JpaRepository<LiveEventAudit, UUID> {

    List<LiveEventAudit> findByRoomIdOrderByOccurredAtAsc(UUID roomId);

    long countByEventType(String eventType);
}
