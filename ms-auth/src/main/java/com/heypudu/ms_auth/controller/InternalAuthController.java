package com.heypudu.ms_auth.controller;

import com.heypudu.ms_auth.service.CognitoAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalAuthController {

    private final CognitoAdminService cognitoAdminService;

    public InternalAuthController(CognitoAdminService cognitoAdminService) {
        this.cognitoAdminService = cognitoAdminService;
    }

    @PutMapping("/{cognitoSub}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable UUID cognitoSub) {
        cognitoAdminService.disableUser(cognitoSub);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{cognitoSub}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable UUID cognitoSub) {
        cognitoAdminService.enableUser(cognitoSub);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cognitoSub}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID cognitoSub) {
        cognitoAdminService.deleteUser(cognitoSub);
        return ResponseEntity.noContent().build();
    }
}