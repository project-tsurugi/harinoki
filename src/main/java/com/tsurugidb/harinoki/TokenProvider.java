package com.tsurugidb.harinoki;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.JWTVerifier;

/**
 * Provides authentication tokens.
 */
public class TokenProvider {

    /**
     * The JWT claim name of authenticated user name.
     */
    public static final String CLAIM_USER_NAME = "tsurugi/auth/name"; //$NON-NLS-1$

    /**
     * The JWT subject value for access tokens.
     */
    public static final String SUBJECT_ACCESS_TOKEN = "access";

    /**
     * The JWT subject value for access tokens.
     */
    public static final String SUBJECT_REFRESH_TOKEN = "refresh";

    private final String issuer;

    private final String audience;

    private final Instant issuedAt;

    private final Duration accessExpiration;

    private final Duration refreshExpiration;

    private final Algorithm algorithm;

    private final JWTVerifier accessTokenVerifier;

    private final JWTVerifier refreshTokenVerifier;

    private final RSAPrivateKey privateKey;

    private final String publicKeyPem;

    /**
     * Creates a new instance.
     * @param issuer the token issuer service name
     * @param audience the token audience service name
     * @param issuedAt the fixed issue date (optional)
     * @param accessExpiration the access expiration period
     * @param refreshExpiration the refresh expiration period
     * @param algorithm the signing algorithm
     * @param privateKey the private key used in token issue
     * @param publicKeyPem the public key string used in the signing
     */
    public TokenProvider(
            @Nonnull String issuer,
            @Nonnull String audience,
            @Nullable Instant issuedAt,
            @Nonnull Duration accessExpiration,
            @Nonnull Duration refreshExpiration,
            @Nonnull Algorithm algorithm,
            @Nonnull RSAPrivateKey privateKey,
            @Nonnull String publicKeyPem) {
        Objects.requireNonNull(issuer);
        Objects.requireNonNull(audience);
        Objects.requireNonNull(accessExpiration);
        Objects.requireNonNull(refreshExpiration);
        Objects.requireNonNull(algorithm);
        Objects.requireNonNull(privateKey);
        Objects.requireNonNull(publicKeyPem);
        this.issuer = issuer;
        this.audience = audience;
        this.issuedAt = issuedAt;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.algorithm = algorithm;
        this.privateKey = privateKey;
        this.publicKeyPem = publicKeyPem;

        this.accessTokenVerifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build();
        this.refreshTokenVerifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(issuer)
                .build();
    }

    /**
     * Returns the issuer service name.
     * @return the issuer service name
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the audience service name.
     * @return the audience service name
     */
    public String getAudience() {
        return audience;
    }

    /**
     * Returns the issued date.
     * @return the preset issued date, or the current time-stamp if it is not set
     */
    public Instant getIssuedAt() {
        if (issuedAt == null) {
            return Instant.now();
        }
        return issuedAt;
    }

    /**
     * Returns the expiration duration for access tokens.
     * @return the expiration duration for access tokens
     */
    public Duration getAccessExpiration() {
        return accessExpiration;
    }

    /**
     * Returns the expiration duration for refresh tokens.
     * @return the expiration duration for refresh tokens
     */
    public Duration getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * Returns the signing algorithm.
     * @return the signing algorithm
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns a token verifier for access tokens.
     * @return a token verifier for access tokens
     */
    public JWTVerifier getAccessTokenVerifier() {
        return accessTokenVerifier;
    }

    /**
     * Returns a token verifier for refresh tokens.
     * @return a token verifier for refresh tokens
     */
    public JWTVerifier getRefreshTokenVerifier() {
        return refreshTokenVerifier;
    }

    /**
     * Issues a new token.
     * @param user the user name
     * @param access issues an access token if {@code true}, or a refresh token
     * @return the issued token
     */
    public String issue(@Nonnull String user, boolean access) {
        Objects.requireNonNull(user);
        return issue(user, access, null);
    }

    /**
     * Issues a new token.
     * @param user the user name
     * @param access issues an access token if {@code true}, or a refresh token
     * @param maxExpiration the maximum expiration time
     * @return the issued token
     */
    public String issue(@Nonnull String user, boolean access, @Nullable Duration maxExpiration) {
        Objects.requireNonNull(user);
        var now = getIssuedAt();
        var expiration = access ? getAccessExpiration() : getRefreshExpiration();
        if (maxExpiration != null && expiration.compareTo(maxExpiration) > 0) {
            expiration = maxExpiration;
        }
        var token = JWT.create()
                .withIssuer(getIssuer())
                .withSubject(access ? SUBJECT_ACCESS_TOKEN : SUBJECT_REFRESH_TOKEN)
                .withAudience(access ? getAudience() : getIssuer())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(expiration)))
                .withClaim(CLAIM_USER_NAME, user)
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
        return token;
    }

    String publicKeyPem() {
        return publicKeyPem;
    }

    String decrypto(String crypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(cipher.doFinal(Base64.getDecoder().decode(crypted)), StandardCharsets.UTF_8);
    }
}
