package com.invoiceflow.shared.security;

import com.invoiceflow.shared.tenancy.TenantId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Turns a validated JWT into an authentication. Tokens without a well-formed tenant claim are rejected
 * outright, so every authenticated request has a tenant. Unknown role names are ignored.
 */
class TenantJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        requireTenant(jwt);
        return new JwtAuthenticationToken(jwt, authorities(jwt), jwt.getSubject());
    }

    private static void requireTenant(Jwt jwt) {
        String tenant = jwt.getClaimAsString(JwtClaims.TENANT_ID);
        try {
            TenantId.of(tenant);
        } catch (RuntimeException e) {
            throw new InvalidBearerTokenException("Token has no valid " + JwtClaims.TENANT_ID + " claim", e);
        }
    }

    private static Collection<GrantedAuthority> authorities(Jwt jwt) {
        List<String> roles = Optional.ofNullable(jwt.getClaimAsStringList(JwtClaims.ROLES)).orElse(List.of());
        return roles.stream()
                .map(Role::fromClaim)
                .flatMap(Optional::stream)
                .distinct()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
    }
}
