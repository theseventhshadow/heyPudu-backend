package com.heypudu.ms_live.service;

import com.heypudu.ms_live.dto.request.CreateLiveRoomRequest;
import com.heypudu.ms_live.dto.response.LiveRoomAccessResponse;
import com.heypudu.ms_live.dto.response.LiveRoomResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

public interface LiveRoomService {
    LiveRoomResponse createRoom(CreateLiveRoomRequest request, Jwt jwt);
    void closeRoom(UUID roomId, Jwt jwt);
    List<LiveRoomResponse> listActiveRooms();
    LiveRoomResponse getRoom(UUID roomId);
    LiveRoomAccessResponse checkAccess(UUID roomId, Jwt jwt);
}
