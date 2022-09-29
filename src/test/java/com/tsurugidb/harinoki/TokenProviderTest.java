package com.tsurugidb.harinoki;

import static org.junit.jupiter.api.Assertions.*;
import static com.tsurugidb.harinoki.TokenProvider.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

class TokenProviderTest {

    @Test
    void access() {
        Algorithm algorithm = Algorithm.HMAC256("secret");
        var provider = new TokenProvider(
                "a",
                "b",
                Instant.ofEpochSecond(2),
                Duration.ofSeconds(3),
                Duration.ofSeconds(4),
                algorithm);

        String token = provider.issue("u", true);
        DecodedJWT jwt = JWT.decode(token);
        assertEquals("a", jwt.getIssuer());
        assertEquals(List.of("b"), jwt.getAudience());
        assertEquals(SUBJECT_ACCESS_TOKEN, jwt.getSubject());
        assertEquals(Instant.ofEpochSecond(2), jwt.getIssuedAt().toInstant());
        assertEquals(Instant.ofEpochSecond(5), jwt.getExpiresAt().toInstant());
        assertEquals("u", jwt.getClaim(CLAIM_USER_NAME).asString());
        algorithm.verify(jwt);
    }

    @Test
    void refresh() {
        Algorithm algorithm = Algorithm.HMAC256("secret");
        var provider = new TokenProvider(
                "a",
                "b",
                Instant.ofEpochSecond(2),
                Duration.ofSeconds(3),
                Duration.ofSeconds(4),
                algorithm);

        String token = provider.issue("u", false);
        DecodedJWT jwt = JWT.decode(token);
        assertEquals("a", jwt.getIssuer());
        assertEquals(List.of("a"), jwt.getAudience());
        assertEquals(SUBJECT_REFRESH_TOKEN, jwt.getSubject());
        assertEquals(Instant.ofEpochSecond(2), jwt.getIssuedAt().toInstant());
        assertEquals(Instant.ofEpochSecond(6), jwt.getExpiresAt().toInstant());
        assertEquals("u", jwt.getClaim(CLAIM_USER_NAME).asString());
        algorithm.verify(jwt);
    }

    @Test
    void expiration() {
        Algorithm algorithm = Algorithm.HMAC256("secret");
        var provider = new TokenProvider(
                "a",
                "b",
                Instant.ofEpochSecond(2),
                Duration.ofSeconds(100),
                Duration.ofSeconds(200),
                algorithm);

        String token = provider.issue("u", true, Duration.ofSeconds(10));
        DecodedJWT jwt = JWT.decode(token);
        assertEquals(Instant.ofEpochSecond(2), jwt.getIssuedAt().toInstant());
        assertEquals(Instant.ofEpochSecond(12), jwt.getExpiresAt().toInstant());
        algorithm.verify(jwt);
    }

    @Test
    void verifier_access() {
        Algorithm algorithm = Algorithm.HMAC256("secret");
        var provider = new TokenProvider(
                "a",
                "b",
                Instant.now(),
                Duration.ofSeconds(10),
                Duration.ofSeconds(-1),
                algorithm);

        String token = provider.issue("u", true);
        provider.getAccessTokenVerifier().verify(token);
    }

    @Test
    void verifier_refresh() {
        Algorithm algorithm = Algorithm.HMAC256("secret");
        var provider = new TokenProvider(
                "a",
                "b",
                Instant.now(),
                Duration.ofSeconds(-1),
                Duration.ofSeconds(10),
                algorithm);

        String token = provider.issue("u", false);
        provider.getRefreshTokenVerifier().verify(token);
    }
}
