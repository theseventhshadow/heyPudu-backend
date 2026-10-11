package com.heypudu.ms_posts.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String COGNITO_GROUPS_CLAIM = "cognito:groups";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> defaultAuthorities = defaultConverter.convert(jwt);
        Collection<GrantedAuthority> cognitoAuthorities = extractCognitoGroups(jwt);

        Set<GrantedAuthority> authorities = new HashSet<>();
        if (defaultAuthorities != null) {
            authorities.addAll(defaultAuthorities);
        }
        authorities.addAll(cognitoAuthorities);

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<GrantedAuthority> extractCognitoGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList(COGNITO_GROUPS_CLAIM);
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        return groups.stream()
                .map(group -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + group.toUpperCase()))
                .toList();
    }
}