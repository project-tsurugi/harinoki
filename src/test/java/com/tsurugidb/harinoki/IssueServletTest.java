package com.tsurugidb.harinoki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.algorithms.Algorithm;

class IssueServletTest {

    private static final TokenProvider DEFAULT_PROVIDER = new TokenProvider(
            "i", "a", null, Duration.ofSeconds(100), Duration.ofSeconds(200), Algorithm.none());

    private static TestingServer server = new TestingServer(18080);

    private final HttpUtil http = new HttpUtil(18080);

    @BeforeAll
    static void start() throws Exception {
        server.getContext().setAttribute(ConfigurationHandler.ATTRIBUTE_TOKEN_PROVIDER, DEFAULT_PROVIDER);
        server.addUser("u", "p", HttpUtil.ROLE_NAME);
        server.start();
    }

    @AfterAll
    static void stop() throws Exception {
        server.close();
    }

    @Test
    void ok() throws Exception {
        Response response = http.get("/issue", "u", "p");
        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertNotNull(response.token);
    }

    @Test
    void no_auth() throws Exception {
        Response response = http.get("/issue");
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.AUTH_ERROR, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void failure() throws Exception {
        Response response = http.get("/issue", "u", "X");
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.AUTH_ERROR, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }
}
