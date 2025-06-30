package com.tsurugidb.harinoki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.algorithms.Algorithm;

class VerifyServletTest {

    private static final TokenProvider DEFAULT_PROVIDER = new TokenProvider(
            "i", "a", null, Duration.ofSeconds(100), Duration.ofSeconds(200),
            Algorithm.RSA256(TokenProviderFactory.createPublicKey(Constants.PUBLIC_KEY), TokenProviderFactory.createPrivateKey(Constants.PRIVATE_KEY)),
            TokenProviderFactory.createPrivateKey(Constants.PRIVATE_KEY), Constants.PUBLIC_KEY);

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
    void ok_access() throws Exception {
        String token = DEFAULT_PROVIDER.issue("u", true);

        Response response = http.submit("/verify", token);
        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertEquals(token, response.token);
    }

    @Test
    void ok_refresh() throws Exception {
        String token = DEFAULT_PROVIDER.issue("u", false);

        Response response = http.submit("/verify", token);
        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertEquals(token, response.token);
    }

    @Test
    void no_token() throws Exception {
        Response response = http.get("/verify");
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.NO_TOKEN, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void basic_auth() throws Exception {
        Response response = http.get("/verify", "u", "p");
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.NO_TOKEN, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void bad_token() throws Exception {
        Response response = http.submit("/verify", "BROKEN");
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.INVALID_TOKEN, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void expired_token() throws Exception {
        String token = new TokenProvider(
                "i", "a", null, Duration.ofSeconds(-1), Duration.ofSeconds(200),
                Algorithm.RSA256(TokenProviderFactory.createPublicKey(Constants.PUBLIC_KEY), TokenProviderFactory.createPrivateKey(Constants.PRIVATE_KEY)),
                TokenProviderFactory.createPrivateKey(Constants.PRIVATE_KEY), Constants.PUBLIC_KEY)
                .issue("u", true);

        Response response = http.submit("/verify", token);
        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertEquals(token, response.token);
    }

    @Test
    void invalid_token() throws Exception {
        String token = new TokenProvider(
                "X", "a", null, Duration.ofSeconds(100), Duration.ofSeconds(200),
                Algorithm.RSA256(TokenProviderFactory.createPublicKey(Constants.PUBLIC_KEY), TokenProviderFactory.createPrivateKey(Constants.PRIVATE_KEY)),
                TokenProviderFactory.createPrivateKey(Constants.PRIVATE_KEY), Constants.PUBLIC_KEY)
                .issue("u", false);

        Response response = http.submit("/verify", token);
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.INVALID_TOKEN, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }
}
