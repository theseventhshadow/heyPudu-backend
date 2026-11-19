package com.heypudu.ms_live.controller;

import com.heypudu.ms_live.dto.request.CreateLiveRoomRequest;
import com.heypudu.ms_live.dto.request.SendReactionRequest;
import com.heypudu.ms_live.dto.response.LiveRoomAccessResponse;
import com.heypudu.ms_live.dto.response.LiveRoomResponse;
import com.heypudu.ms_live.service.LiveRoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/live/rooms")
public class LiveRoomController {

    private final LiveRoomService liveRoomService;

    public LiveRoomController(LiveRoomService liveRoomService) {
        this.liveRoomService = liveRoomService;
    }

    @PostMapping
    public ResponseEntity<LiveRoomResponse> createRoom(
            @Valid @RequestBody CreateLiveRoomRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(liveRoomService.createRoom(request, jwt));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> closeRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        liveRoomService.closeRoom(roomId, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<LiveRoomResponse>> listActiveRooms() {
        return ResponseEntity.ok(liveRoomService.listActiveRooms());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<LiveRoomResponse> getRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(liveRoomService.getRoom(roomId));
    }

    @GetMapping("/{roomId}/access")
    public ResponseEntity<LiveRoomAccessResponse> checkAccess(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(liveRoomService.checkAccess(roomId, jwt));
    }

    @PostMapping("/{roomId}/reactions")
    public ResponseEntity<Void> sendReaction(
            @PathVariable UUID roomId,
            @Valid @RequestBody SendReactionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        liveRoomService.sendReaction(roomId, request.type(), jwt);
        return ResponseEntity.accepted().build();
    }
}
