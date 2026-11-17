package com.heypudu.ms_live.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CognitoUserExtractor {

    public UUID extractSub(Jwt jwt) {
        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("El JWT no contiene el claim 'sub'");
        }
        return UUID.fromString(sub);
    }
}
