package com.tsurugidb.harinoki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.algorithms.Algorithm;

class EncryptionKeyServletTest {

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
        Response response = http.get("/encryption-key");
        assertEquals(200, response.code, response::toString);
        assertEquals("RSA", response.key_type);
        assertEquals(TestConstants.publicKey(), response.key_data);
        assertNull(response.token);
    }
}
