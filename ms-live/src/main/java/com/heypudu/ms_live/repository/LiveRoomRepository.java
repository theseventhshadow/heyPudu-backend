package com.heypudu.ms_live.repository;

import com.heypudu.ms_live.model.LiveRoom;
import com.heypudu.ms_live.model.LiveRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveRoomRepository extends JpaRepository<LiveRoom, UUID> {
    Optional<LiveRoom> findByCreatorCognitoSubAndStatus(UUID creatorCognitoSub, LiveRoomStatus status);
    List<LiveRoom> findByStatusOrderByStartedAtDesc(LiveRoomStatus status);
}
