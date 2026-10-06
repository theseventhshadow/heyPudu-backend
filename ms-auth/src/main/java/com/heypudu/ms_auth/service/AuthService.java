package com.heypudu.ms_auth.service;

import com.heypudu.ms_auth.dto.response.AuthUserResponse;
import com.heypudu.ms_auth.dto.response.SyncUserResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {

    /**
     * Devuelve los datos del usuario autenticado a partir del JWT.
     * Busca el registro en la BD por cognito_sub.
     * Si no existe, lanza ResourceNotFoundException.
     */
    AuthUserResponse getCurrentUser(Jwt jwt);

    /**
     * Sincroniza el usuario de Cognito con la BD de ms-auth.
     * Si el usuario ya existe (por cognito_sub), devuelve el registro existente con created = false.
     * Si no existe, lo crea con status ACTIVE y devuelve created = true.
     */
    SyncUserResponse syncUser(Jwt jwt);
}