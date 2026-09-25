package com.invoiceflow.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Replaces the identity provider in tests: tokens are signed with a local HMAC key and verified by the
 * real resource-server filter chain, so claim handling is exercised end to end.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwt {

    private static final SecretKey KEY = new SecretKeySpec(
            "invoiceflow-test-signing-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    @Bean
    JwtDecoder testJwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(KEY).build();
    }

    /** A signed bearer token for {@code subject} in {@code tenant} holding {@code roles}. */
    public static String token(String subject, String tenant, String... roles) {
        return sign(new JWTClaimsSet.Builder().claim("tenant_id", tenant).claim("roles", List.of(roles)), subject);
    }

    /** A signed bearer token with no tenant claim. */
    public static String tokenWithoutTenant(String subject, String... roles) {
        return sign(new JWTClaimsSet.Builder().claim("roles", List.of(roles)), subject);
    }

    /** An authentication equivalent to a validated token, for tests that bypass HTTP. */
    public static JwtAuthenticationToken authentication(String subject, String tenant) {
        Jwt jwt = Jwt.withTokenValue("test")
                .header("alg", "HS256")
                .subject(subject)
                .claims(claims -> claims.putAll(Map.of("tenant_id", tenant)))
                .build();
        return new JwtAuthenticationToken(jwt, List.of(), subject);
    }

    private static String sign(JWTClaimsSet.Builder claims, String subject) {
        Instant now = Instant.now();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build());
        try {
            jwt.sign(new MACSigner(KEY));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return jwt.serialize();
    }
}
