package com.heypudu.ms_auth.controller;

import com.heypudu.ms_auth.dto.response.AuthUserResponse;
import com.heypudu.ms_auth.dto.response.SyncUserResponse;
import com.heypudu.ms_auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Devuelve los datos del usuario autenticado a partir del JWT.
     * No crea ni modifica nada en la BD.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        AuthUserResponse response = authService.getCurrentUser(jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * Sincroniza el usuario de Cognito con la BD de ms-auth.
     * Se llama una sola vez, tras el primer login exitoso en Cognito.
     * Si el usuario ya existe, devuelve el registro existente.
     * Si no existe, lo crea.
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncUserResponse> syncUser(@AuthenticationPrincipal Jwt jwt) {
        SyncUserResponse response = authService.syncUser(jwt);
        return ResponseEntity.ok(response);
    }
}