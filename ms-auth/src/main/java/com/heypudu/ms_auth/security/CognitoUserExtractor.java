package com.heypudu.ms_auth.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CognitoUserExtractor {

    private static final String SUB_CLAIM = "sub";
    private static final String EMAIL_CLAIM = "email";
    private static final String GROUPS_CLAIM = "cognito:groups";

    public UUID extractSub(Jwt jwt) {
        String sub = jwt.getClaimAsString(SUB_CLAIM);
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("El JWT no contiene el claim 'sub'");
        }
        return UUID.fromString(sub);
    }

    public String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El JWT no contiene el claim 'email'");
        }
        return email;
    }

    public List<String> extractGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList(GROUPS_CLAIM);
        return groups != null ? groups : List.of();
    }
}