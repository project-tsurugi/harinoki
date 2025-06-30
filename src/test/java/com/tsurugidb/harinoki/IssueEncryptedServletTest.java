package com.tsurugidb.harinoki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.auth0.jwt.algorithms.Algorithm;

class IssueEncryptedServletTest {
    private static final RSAPrivateKey privateKey = TokenProviderFactory.createPrivateKey(Constants.PRIVATE_KEY);

    private static final RSAPublicKey publicKey = TokenProviderFactory.createPublicKey(Constants.PUBLIC_KEY);

    private static final TokenProvider DEFAULT_PROVIDER = new TokenProvider(
            "i", "a", null, Duration.ofSeconds(100), Duration.ofSeconds(200), Algorithm.RSA256(publicKey, privateKey),
            privateKey, Constants.PUBLIC_KEY);

    private static TestingServer server = new TestingServer(18080);

    private final HttpUtil http = new HttpUtil(18080);

    private final String U = "X4CdQ93SSnuR12X6SigWLXj1RCFsrdgV9eTS++QqUeINPQz9WnuOL0gX42cosc29+/sn95n/fa0StT9cIKMpBcr1CklJfDSq0CvBcqV0zPzMruHxxV16AFJmE6HiIvBXTbclCzdTYgQGTnkrvLLxnOFbuq+NgeZ/e4FrkF1NQwUC7J46gSv/QnjvPnSRfiBhjGJih2IaqLMi2HbZykCAszrJdndEAgZ4pp+zEpIIoHyl4d6kvubsVoAMW/QhBvx83GGHHyGKVllYBrNAYIdIG7T26ibI9lls1nR4B4TwB9oPAnmrQp6pLDgK1y4mLqLL3Whe3X6HBziHquFC37wmgg";
    private final String P = "d11VGrIp9JJe+N0zyTpAERLg+OBAuxzemMEbYRLjQGfIMvnqReXxlT6x9+EPAMrkW/k6GuRtgHDBA1ZaJzxp9nWPE/L2s0lnDOo/MJN38BhVAGeFonxilfDf7zRiuzxfv11jUwCubwBMznM9447Hz8u10xM5WUtSkiNjxRHIyPvO29VpSj/s+DtA5p1KaHXwKkgdxEq+LLixwnLZf1H+ZJ2kiJ8MK0Ip2Br4HooBVLbNrAdxBDz08uDqZ/ktqe4l9pICzSgf8tR+z0bA06pyEPKG9jT8y+uwqqlXdaCfp1vg1+W+X2HfM1nzY85jVHZmCpZDlpvDK6xrqkP1KCvf+A";
    private final String X = "hxtMaeTeXA0m13M5D462serJxdYJpUpViD18WvJlILiDSisb65aVCKIigr4TOrJEKCc2qiPsKk9IQ2xXIFvF20RLZM66AkmqFODlVS2aTWLAAhkRQ4h+pyF7QZ4P3EEPjYQZNGfeeLfRm4yaZfSDstlnrqMcion1A9VP5tgA0syg07iRIKUu3dZ3dRtiScuXz6xnIv6WrK4ldfv9r3MIBc748wihUamd4xljKrBEXZDhJTcQbZTBR2zGgld2M2hhfKewtkpIkQt2a/B9d7RlpAZOFM2D2bJSaLkkA0OFVLowlgm9RZYaD+MVDPVznvNQrN7LB2R6kt88NrqoSiMQTQ";

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
        Response response = http.get("/issue-encrypted", U + "." + P);
        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertNotNull(response.token);
    }

    @Test
    void no_auth() throws Exception {
        Response response = http.get("/issue-encrypted");
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.AUTH_ERROR, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void failure() throws Exception {
        Response response = http.get("/issue-encrypted", U + "." + X);
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.AUTH_ERROR, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }
}
