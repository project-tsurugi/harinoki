package com.tsurugidb.harinoki;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    /**
     * Returns a new {@link TokenProvider}.
     * @return the created token provider
     * @throws IOException if error was occurred while creating the instance
     */
    public TokenProvider newInstance() throws IOException {
        return new TokenProvider(
                load(KEY_ISSUER, DEFAULT_ISSUER),
                load(KEY_AUDIENCE, DEFAULT_AUDIENCE),
                null, // issued at
                load(KEY_ACCESS_EXPIRATION, DEFAULT_ACCESS_EXPIRATION),
                load(KEY_REFRESH_EXPIRATION, DEFAULT_REFRESH_EXPIRATION),
                Algorithm.HMAC256(load(KEY_SECRET, (String) null)));
    }

    /**
     * Obtains configuration value from environment variables.
     * @param key the variable name
     * @param defaultValue the default value
     * @return the environment variable
     */
    String getEnvironmentVariable(@Nonnull String key, @Nullable String defaultValue) {
        LOG.trace("loading environment variable: {}", key); //$NON-NLS-1$
        String value = Optional.ofNullable(System.getenv(key))
                .map(String::strip)
                .orElse(null);
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
}
