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
            "i", "a", null, Duration.ofSeconds(100), Duration.ofSeconds(200), Algorithm.none());

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
        assertNotNull(token, response.token);

        DecodedJWT jwt = JWT.decode(response.token);
        assertTrue(TokenUtil.isAccessToken(jwt));
        assertEquals("u", TokenUtil.getUserName(jwt));
    }

    @Test
    void access_token() throws Exception {
        String token = DEFAULT_PROVIDER.issue("u", true);

        Response response = http.submit("/refresh", token);
        assertEquals(403, response.code, response::toString);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void no_token() throws Exception {
        Response response = http.get("/refresh");
        assertEquals(401, response.code, response::toString);
        assertNull(response.token);
        assertNotNull(response.message);
    }
}
