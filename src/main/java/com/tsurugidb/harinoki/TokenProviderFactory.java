package com.tsurugidb.harinoki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
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
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.Set;
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

    public static final String KEY_ISSUER = "tsurugi.jwt.claim_iss"; //$NON-NLS-1$

    public static final String KEY_AUDIENCE = "tsurugi.jwt.claim_aud"; //$NON-NLS-1$

    public static final String KEY_PRIVATE_KEY = "tsurugi.jwt.private_key_file"; //$NON-NLS-1$

    public static final String KEY_ACCESS_EXPIRATION = "tsurugi.token.expiration"; //$NON-NLS-1$

    public static final String KEY_REFRESH_EXPIRATION = "tsurugi.token.expiration_refresh"; //$NON-NLS-1$

    public static final String DEFAULT_ISSUER = "harinoki"; //$NON-NLS-1$

    public static final String DEFAULT_AUDIENCE = "tsurugidb"; //$NON-NLS-1$

    public static final String DEFAULT_PRIVATE_KEY = "harinoki.pem"; //$NON-NLS-1$

    public static final Duration DEFAULT_ACCESS_EXPIRATION = Duration.ofSeconds(300);

    public static final Duration DEFAULT_REFRESH_EXPIRATION = Duration.ofHours(24);

    private Path propertyFilePath = null;

    // cache of propertyFilePath.getParent()
    // It is set to protected because the subclass writes to it during testing.
    protected Path base = null;

    private Properties properties = new Properties();

    void initializeAndCheck() {
        String propertyFile = System.getenv("HARINOKI_PROPERTIES");
        boolean noError = false;
        if (propertyFile != null) {
            if (propertyFile.startsWith(File.pathSeparator)) {
                propertyFilePath = Path.of(propertyFile);
            } else {
                propertyFilePath = Path.of("/", propertyFile);
            }
            noError = true;
        }
        String dir = System.getenv("TSURUGI_HOME");
        if (dir != null) {
            propertyFilePath = Path.of(dir.toString(), "var", "auth", "etc", "harinoki.properties");
            noError = true;
        }
        if (!noError) {
            throw new ServiceConfigurationError("both HARINOKI_PROPERTIES and TSURUGI_HOME are not set");
        }

        // check for the base directory
        base = propertyFilePath.getParent();
        if (base == null) {
            throw new ServiceConfigurationError(MessageFormat.format("invalid parent directory of {0}", propertyFilePath));
        }
        if (!checkPermission(base)) {
            throw new ServiceConfigurationError(MessageFormat.format("invalid permission: {0}", base));
        }

        // check for the property file
        if (!Files.exists(propertyFilePath)) {
            throw new ServiceConfigurationError(MessageFormat.format("cannot find the property file: {0}", propertyFilePath));
        }
        if (!checkPermission(propertyFilePath)) {
            throw new ServiceConfigurationError(MessageFormat.format("invalid permission: {0}", propertyFilePath));
        }
        try (FileInputStream fileInputStream = new FileInputStream(new File(propertyFilePath.toString()))) {
            try {
                properties.load(fileInputStream);
            } catch (IOException e) {
                throw new ServiceConfigurationError(MessageFormat.format("failed to load property from {0}", propertyFilePath));
            }
        } catch (IOException e) {
            throw new ServiceConfigurationError(MessageFormat.format("cannot create FileInputStream of {0}", propertyFilePath));
        }
        try {
            // check for the key file
            Path keyFile = Path.of(base.toString(), load(KEY_PRIVATE_KEY, DEFAULT_PRIVATE_KEY));
            if (Files.exists(keyFile)) {
                if (!checkPermission(keyFile)) {
                    throw new ServiceConfigurationError(MessageFormat.format("invalid permission: {0}", keyFile));
                }
            } else {
                throw new ServiceConfigurationError(MessageFormat.format("cannot find the key file: {0}", keyFile));
            }
        } catch (IOException e) {
            throw new ServiceConfigurationError(e.getMessage());
        }
    }
    private boolean checkPermission(Path filePath) {
        try {
            PosixFileAttributes fileAttributes = Files.readAttributes(filePath, PosixFileAttributes.class);

            Set<PosixFilePermission> permissions = fileAttributes.permissions();

            boolean groupRead = permissions.contains(PosixFilePermission.GROUP_READ);
            boolean groupWrite = permissions.contains(PosixFilePermission.GROUP_WRITE);
            boolean groupExecute = permissions.contains(PosixFilePermission.GROUP_EXECUTE);

            boolean othersRead = permissions.contains(PosixFilePermission.OTHERS_READ);
            boolean othersWrite = permissions.contains(PosixFilePermission.OTHERS_WRITE);
            boolean othersExecute = permissions.contains(PosixFilePermission.OTHERS_EXECUTE);

            return !(groupRead || groupWrite || groupExecute || othersRead || othersWrite || othersExecute);

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns a new {@link TokenProvider}.
     * @return the created token provider
     * @throws IOException if error was occurred while creating the instance
     */
    public TokenProvider newInstance() throws IOException {
        String privateKeyFile = load(KEY_PRIVATE_KEY, DEFAULT_PRIVATE_KEY);
        RSAPrivateKey privateKey = createPrivateKey(privateKey(Path.of(base.toString(), privateKeyFile)));
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

    String privateKey(Path pemFile) throws IOException {
        try {
            return inputStreamToString(new FileInputStream(pemFile.toString()));
        } catch (FileNotFoundException e) {
            throw new IOException(MessageFormat.format(
                "cannot create InputStream from pem file: {0}",
                pemFile));
        }
    }

    private static String inputStreamToString(InputStream input) throws IOException {
        StringBuilder builder = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(input, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    /**
     * Obtains configuration value from environment variables.
     * @param key the variable name
     * @param defaultValue the default value
     * @return the environment variable
     */
    String getEnvironmentVariable(@Nonnull String key, @Nullable String defaultValue) {
        LOG.trace("loading environment variable: {}", key); //$NON-NLS-1$
        String value = Optional.ofNullable(properties.getProperty(key))
                .map(String::strip)
                .orElse(null);
        if (value != null) {
            if (LOG.isDebugEnabled()) {
                if (key.equals(KEY_PRIVATE_KEY)) {
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

    protected String load(@Nonnull String key, @Nullable String defaultValue) throws IOException {
        String value = getEnvironmentVariable(key, defaultValue);
        if (value != null) {
            return value;
        }
        throw new IOException(MessageFormat.format(
                "property value is not set: {0}",
                key));
    }

    private Duration load(@Nonnull String key, @Nonnull Duration defaultValue) throws IOException {
        LOG.trace("loading environment variable: {}", key); //$NON-NLS-1$
        String value = getEnvironmentVariable(key, null);
        if (value != null) {
            LOG.debug("environment variable: \"{}\" = \"{}\"", key, value);
            Matcher matcher = PATTERN_DURATION.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(MessageFormat.format(
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
