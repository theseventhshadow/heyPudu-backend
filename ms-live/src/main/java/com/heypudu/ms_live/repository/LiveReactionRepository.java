package com.heypudu.ms_live.repository;

import com.heypudu.ms_live.model.LiveReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LiveReactionRepository extends JpaRepository<LiveReaction, UUID> {

    long countByRoomId(UUID roomId);

    void deleteByRoomId(UUID roomId);
}
