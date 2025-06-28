package com.tsurugidb.harinoki;

import java.io.IOException;
import java.text.MessageFormat;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.algorithms.Algorithm;

/**
 * Loads token configurations from environment variables and provides {@link TokenProvider}.
 */
class TokenProviderFactory {

    static final Logger LOG = LoggerFactory.getLogger(TokenProviderFactory.class);

    private static final Pattern PATTERN_DURATION = Pattern.compile(
            "(?<time>[1-9][0-9]*)\\s*((?<hour>h(ours?)?)|(?<minute>min(utes?)?)|s(ec(onds?)?)?)?",
            Pattern.CASE_INSENSITIVE);

    public static final String KEY_ISSUER = "TSURUGI_JWT_CLAIM_ISS"; //$NON-NLS-1$

    public static final String KEY_AUDIENCE = "TSURUGI_JWT_CLAIM_AUD"; //$NON-NLS-1$

    public static final String KEY_SECRET = "TSURUGI_JWT_SECRET_KEY"; //$NON-NLS-1$

    public static final String KEY_ACCESS_EXPIRATION = "TSURUGI_TOKEN_EXPIRATION"; //$NON-NLS-1$

    public static final String KEY_REFRESH_EXPIRATION = "TSURUGI_TOKEN_EXPIRATION_REFRESH"; //$NON-NLS-1$

    public static final String DEFAULT_ISSUER = "authentication-manager"; //$NON-NLS-1$

    public static final String DEFAULT_AUDIENCE = "metadata-manager"; //$NON-NLS-1$

    public static final Duration DEFAULT_ACCESS_EXPIRATION = Duration.ofSeconds(300);

    public static final Duration DEFAULT_REFRESH_EXPIRATION = Duration.ofHours(24);

    private Context envCtx;

    TokenProviderFactory() {
        try {
            Context initCtx = new InitialContext();
            this.envCtx = (Context) initCtx.lookup("java:comp/env");
        } catch (NamingException e) {
            this.envCtx = null;
        }
    }

    /**
     * Returns a new {@link TokenProvider}.
     * @return the created token provider
     * @throws IOException if error was occurred while creating the instance
     */
    public TokenProvider newInstance() throws IOException {
        RSAPrivateKey privateKey = createPrivateKey(load(KEY_SECRET, (String) null));
        RSAPublicKey publicKey = createPublicKey(privateKey);
        return new TokenProvider(
                load(KEY_ISSUER, DEFAULT_ISSUER),
                load(KEY_AUDIENCE, DEFAULT_AUDIENCE),
                null, // issued at
                load(KEY_ACCESS_EXPIRATION, DEFAULT_ACCESS_EXPIRATION),
                load(KEY_REFRESH_EXPIRATION, DEFAULT_REFRESH_EXPIRATION),
                Algorithm.RSA256(publicKey, privateKey),
                privateKey,
                publicKeyPem(publicKey));
    }

    /**
     * Obtains configuration value from environment variables.
     * @param key the variable name
     * @param defaultValue the default value
     * @return the environment variable
     */
    String getEnvironmentVariable(@Nonnull String key, @Nullable String defaultValue) {
        LOG.trace("loading environment variable: {}", key); //$NON-NLS-1$
        String value = null;
        try {
            if (envCtx != null) {
                value = Optional.ofNullable((String) envCtx.lookup(key))
                        .map(String::strip)
                        .orElse(null);
            }
        } catch (NamingException e) {
            value = null;
        }
        if (value != null) {
            if (LOG.isDebugEnabled()) {
                if (key.equals(KEY_SECRET)) {
                    LOG.debug("environment variable: \"{}\" = ***", key);
                } else {
                    LOG.debug("environment variable: \"{}\" = \"{}\"", key, value);
                }
            }
            return value;
        }
        if (defaultValue != null) {
            LOG.debug("default value: \"{}\" = \"{}\"", key, defaultValue); //$NON-NLS-1$
            return defaultValue;
        }
        return null;
    }

    private String load(@Nonnull String key, @Nullable String defaultValue) throws IOException {
        String value = getEnvironmentVariable(key, defaultValue);
        if (value != null) {
            return value;
        }
        throw new IOException(MessageFormat.format(
                "environment value is not set: {0}",
                key));
    }

    private Duration load(@Nonnull String key, @Nonnull Duration defaultValue) throws IOException {
        LOG.trace("loading environment variable: {}", key); //$NON-NLS-1$
        String value = getEnvironmentVariable(key, null);
        if (value != null) {
            LOG.debug("environment variable: \"{}\" = \"{}\"", key, value);
            Matcher matcher = PATTERN_DURATION.matcher(value);
            if (!matcher.matches()) {
                throw new IOException(MessageFormat.format(
                        "invalid duration format in \"{0}\": \"{1}\"",
                        key,
                        value));
            }
            long time = Long.parseLong(matcher.group("time"));
            ChronoUnit unit = ChronoUnit.SECONDS;
            if (matcher.group("hour") != null) {
                unit = ChronoUnit.HOURS;
            } else if (matcher.group("minute") != null) { //$NON-NLS-1$
                unit = ChronoUnit.MINUTES;
            }
            LOG.debug("environment variable: \"{}\" = {} {}", key, time, unit);
            return Duration.of(time, unit);
        }
        LOG.debug("default value: \"{}\" = \"{}\"", key, defaultValue);
        return defaultValue;
    }

    /**
     * Rebuild and returns the RSAPublicKey.
     * @param rsaPrivateKey the RSA private key
     * @return the RSAPublicKey
     * @throws IllegalStateException if error was occurred while creating the instance
     */
    public static RSAPublicKey createPublicKey(RSAPrivateKey rsaPrivateKey) {
        try {
            if (rsaPrivateKey instanceof RSAPrivateCrtKey) {
                RSAPrivateCrtKey pk = (RSAPrivateCrtKey) rsaPrivateKey;
                KeyFactory keyFactoryPub = KeyFactory.getInstance("RSA");
                RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(pk.getModulus(), pk.getPublicExponent());
                return (RSAPublicKey) keyFactoryPub.generatePublic(rsaPublicKeySpec);
            }
            throw new AssertionError("rsaPrivateKey is not an instance of RSAPrivateCrtKey");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the RSAPrivateKey.
     * @param pem the key string
     * @return the RSAPrivateKey
     * @throws IllegalStateException if error was occurred while creating the instance
     */
    public static RSAPrivateKey createPrivateKey(String pem) {
        try {
            String keyPem = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");

            byte[] encoded = Base64.getDecoder().decode(keyPem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }
    /**
     * Returns the RSAPublicKey.
     * @param pem the key string
     * @return the RSAPublicKey
     * @throws IllegalStateException if error was occurred while creating the instance
     */
    public static RSAPublicKey createPublicKey(String pem) {
        try {
            String keyPem = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "");

            byte[] encoded = Base64.getDecoder().decode(keyPem);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }
    String publicKeyPem(RSAPublicKey publicKey) {
        byte[] publicKeyBytes = publicKey.getEncoded();

        String pemEncodedPublicKey = "-----BEGIN PUBLIC KEY-----"
            + System.lineSeparator()
            + Base64.getEncoder().withoutPadding().encodeToString(publicKeyBytes)
            + System.lineSeparator()
            + "-----END PUBLIC KEY-----";

        return pemEncodedPublicKey;
    }
}
