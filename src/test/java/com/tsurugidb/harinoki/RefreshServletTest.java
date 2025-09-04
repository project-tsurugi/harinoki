package com.tsurugidb.harinoki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

class RefreshServletTest {

    private static final TokenProvider DEFAULT_PROVIDER = new TokenProvider(
            "i", "a", null, Duration.ofSeconds(100), Duration.ofSeconds(200),
            Algorithm.RSA256(TokenProviderFactory.createPublicKey(TestConstants.publicKey()), TokenProviderFactory.createPrivateKey(TestConstants.privateKey())),
            TokenProviderFactory.createPrivateKey(TestConstants.privateKey()), TestConstants.publicKey());

    private static TestingServer server = new TestingServer(18080);

    private final HttpUtil http = new HttpUtil(18080);

    @BeforeAll
    static void start() throws Exception {
        server.getContext().setAttribute(ConfigurationHandler.ATTRIBUTE_TOKEN_PROVIDER, DEFAULT_PROVIDER);
        server.start();
    }

    @AfterAll
    static void stop() throws Exception {
        server.close();
    }

    @Test
    void ok() throws Exception {
        String token = DEFAULT_PROVIDER.issue("u", false);

        Response response = http.submit("/refresh", token);
        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertNotNull(token, response.token);

        DecodedJWT jwt = JWT.decode(response.token);
        assertTrue(TokenUtil.isAccessToken(jwt));
        assertEquals("u", TokenUtil.getUserName(jwt));
        assertEquals(
                DEFAULT_PROVIDER.getAccessExpiration(),
                Duration.ofMillis(
                        jwt.getExpiresAt().toInstant().toEpochMilli() - jwt.getIssuedAt().toInstant().toEpochMilli()));
    }

    @Test
    void max_expiration() throws Exception {
        String token = DEFAULT_PROVIDER.issue("u", false);

        Response response = http.submit("/refresh", request -> {
            request.header("Authorization", String.format("Bearer %s", token));
            request.header(RefreshServlet.KEY_TOKEN_EXPIRATION, "12");
        });

        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertNotNull(token, response.token);

        DecodedJWT jwt = JWT.decode(response.token);
        assertTrue(TokenUtil.isAccessToken(jwt));
        assertEquals("u", TokenUtil.getUserName(jwt));
        assertEquals(
                Duration.ofSeconds(12),
                Duration.ofMillis(
                        jwt.getExpiresAt().toInstant().toEpochMilli() - jwt.getIssuedAt().toInstant().toEpochMilli()));
    }

    @Test
    void access_token() throws Exception {
        String token = DEFAULT_PROVIDER.issue("u", true);

        Response response = http.submit("/refresh", token);
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.INVALID_AUDIENCE, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void no_token() throws Exception {
        Response response = http.get("/refresh");
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.NO_TOKEN, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void expired_token() throws Exception {
        String token = new TokenProvider(
                "i", "a", null, Duration.ofSeconds(100), Duration.ofSeconds(-1),
                Algorithm.RSA256(TokenProviderFactory.createPublicKey(TestConstants.publicKey()), TokenProviderFactory.createPrivateKey(TestConstants.privateKey())),
                TokenProviderFactory.createPrivateKey(TestConstants.privateKey()), TestConstants.publicKey())
                .issue("u", false);

        Response response = http.submit("/refresh", token);
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.TOKEN_EXPIRED, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }
}
