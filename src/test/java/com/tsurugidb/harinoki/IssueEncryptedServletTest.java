package com.tsurugidb.harinoki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonFactoryBuilder;

import com.auth0.jwt.algorithms.Algorithm;

class IssueEncryptedServletTest {
    private static final RSAPrivateKey privateKey = TokenProviderFactory.createPrivateKey(Constants.privateKey());

    private static final RSAPublicKey publicKey = TokenProviderFactory.createPublicKey(Constants.publicKey());

    private static final TokenProvider DEFAULT_PROVIDER = new TokenProvider(
            "i", "a", null, Duration.ofSeconds(100), Duration.ofSeconds(200), Algorithm.RSA256(publicKey, privateKey),
            privateKey, Constants.publicKey());

    private static TestingServer server = new TestingServer(18080);

    private final HttpUtil http = new HttpUtil(18080);

    private static String encryptoByPublicKey(String text) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return Base64.getEncoder().withoutPadding().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static final JsonFactory JSON = new JsonFactoryBuilder().build();

    public static final String KEY_FORMAT_VERSION = "format_version";

    public static final String KEY_USER = "user";

    public static final String KEY_PASSWORD = "password";

    public static final String KEY_EXPIRATION_DATE = "expiration_date";

    private static String getJsonText(String user, String password, String expirationDate) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (var writer = JSON.createGenerator(stringWriter)) {
            writer.writeStartObject();
            writer.writeFieldName(KEY_FORMAT_VERSION);
            writer.writeNumber(1);
            writer.writeFieldName(KEY_USER);
            writer.writeString(user);
            writer.writeFieldName(KEY_PASSWORD);
            writer.writeString(password != null ? password : "");
            if (expirationDate != null) {
                writer.writeFieldName(KEY_EXPIRATION_DATE);
                writer.writeString(expirationDate);
            }
            writer.writeEndObject();
        }
        return stringWriter.toString();
    }

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
        Response response = http.get("/issue-encrypted", encryptoByPublicKey(getJsonText("u", "p", null)));
        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertNotNull(response.token);
    }

    @Test
    void ok_before_di() throws Exception {
        var di = Instant.now().plusSeconds(3600);
        Response response = http.get("/issue-encrypted", encryptoByPublicKey(getJsonText("u", "p", di.toString())));
        assertEquals(200, response.code, response::toString);
        assertEquals(MessageType.OK, response.type);
        assertNotNull(response.token);
    }

    @Test
    void ng_after_di() throws Exception {
        var di = Instant.now().minusSeconds(3600);
        Response response = http.get("/issue-encrypted", encryptoByPublicKey(getJsonText("u", "p", di.toString())));
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.AUTH_ERROR, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }

    @Test
    void ng_invalid_di() throws Exception {
        var di = Instant.now().plusSeconds(3600);
        Response response = http.get("/issue-encrypted", encryptoByPublicKey(getJsonText("u", "p", di.toString().replace("T", ""))));
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.AUTH_ERROR, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
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
        Response response = http.get("/issue-encrypted", encryptoByPublicKey(getJsonText("u", "x", null)));
        assertEquals(401, response.code, response::toString);
        assertEquals(MessageType.AUTH_ERROR, response.type);
        assertNull(response.token);
        assertNotNull(response.message);
    }
}
