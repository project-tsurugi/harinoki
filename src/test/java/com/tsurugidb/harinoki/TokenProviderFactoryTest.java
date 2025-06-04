package com.tsurugidb.harinoki;

import static org.junit.jupiter.api.Assertions.*;
import static com.tsurugidb.harinoki.TokenProviderFactory.*;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.algorithms.Algorithm;

class TokenProviderFactoryTest {

    static class Mock extends TokenProviderFactory {

        private final Map<String, String> env;

        Mock(Map<String, String> env) {
            this.env = env;
        }

        @Override
        String getEnvironmentVariable(String key, String defaultValue) {
            return env.getOrDefault(key, defaultValue);
        }
    }

    @Test
    void simple() throws Exception {
        var mock = new Mock(Map.of(
                KEY_ISSUER, "a",
                KEY_AUDIENCE, "b",
                KEY_SECRET, Constants.SECRET_KEY,
                KEY_ACCESS_EXPIRATION, "1",
                KEY_REFRESH_EXPIRATION, "2"));

        TokenProvider provider = mock.newInstance();
        assertEquals("a", provider.getIssuer());
        assertEquals("b", provider.getAudience());
        assertArrayEquals(
                Algorithm.RSA256(TokenProviderFactory.createPublicKey(Constants.PUBLIC_KEY), TokenProviderFactory.createPrivateKey(Constants.SECRET_KEY)).sign(new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 }),
                provider.getAlgorithm().sign(new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 }));
        assertEquals(Duration.ofSeconds(1), provider.getAccessExpiration());
        assertEquals(Duration.ofSeconds(2), provider.getRefreshExpiration());
    }

    @Test
    void defaults() throws Exception {
        var mock = new Mock(Map.of(
                KEY_SECRET, Constants.SECRET_KEY));

        TokenProvider provider = mock.newInstance();
        assertEquals(DEFAULT_ISSUER, provider.getIssuer());
        assertEquals(DEFAULT_AUDIENCE, provider.getAudience());
        assertEquals(DEFAULT_ACCESS_EXPIRATION, provider.getAccessExpiration());
        assertEquals(DEFAULT_REFRESH_EXPIRATION, provider.getRefreshExpiration());
    }

    @Test
    void duration_hour() throws Exception {
        var mock = new Mock(Map.of(
                KEY_SECRET, Constants.SECRET_KEY,
                KEY_ACCESS_EXPIRATION, "2h",
                KEY_REFRESH_EXPIRATION, "3hours"));

        TokenProvider provider = mock.newInstance();
        assertEquals(Duration.ofHours(2), provider.getAccessExpiration());
        assertEquals(Duration.ofHours(3), provider.getRefreshExpiration());
    }

    @Test
    void duration_minute() throws Exception {
        var mock = new Mock(Map.of(
                KEY_SECRET, Constants.SECRET_KEY,
                KEY_ACCESS_EXPIRATION, "2min",
                KEY_REFRESH_EXPIRATION, "3minutes"));

        TokenProvider provider = mock.newInstance();
        assertEquals(Duration.ofMinutes(2), provider.getAccessExpiration());
        assertEquals(Duration.ofMinutes(3), provider.getRefreshExpiration());
    }

    @Test
    void duration_seconds() throws Exception {
        var mock = new Mock(Map.of(
                KEY_SECRET, Constants.SECRET_KEY,
                KEY_ACCESS_EXPIRATION, "2s",
                KEY_REFRESH_EXPIRATION, "3seconds"));

        TokenProvider provider = mock.newInstance();
        assertEquals(Duration.ofSeconds(2), provider.getAccessExpiration());
        assertEquals(Duration.ofSeconds(3), provider.getRefreshExpiration());
    }

    @Test
    void missing_secret() throws Exception {
        var mock = new Mock(Map.of());
        assertThrows(IOException.class, () -> mock.newInstance());
    }

    @Test
    void invalid_duration() throws Exception {
        var mock = new Mock(Map.of(
                KEY_SECRET, "c",
                KEY_ACCESS_EXPIRATION, "2c"));

        assertThrows(IllegalArgumentException.class, () -> mock.newInstance());
    }
}
