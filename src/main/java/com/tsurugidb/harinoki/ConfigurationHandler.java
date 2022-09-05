package com.tsurugidb.harinoki;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Handles authentication service configurations.
 */
public class ConfigurationHandler implements ServletContextListener {

    static final Logger LOG = LoggerFactory.getLogger(ConfigurationHandler.class);

    /**
     * The attribute name of {@link TokenProvider} instance.
     */
    public static final String ATTRIBUTE_TOKEN_PROVIDER = TokenProvider.class.getName();

    private static final TokenProviderFactory FACTORY = new TokenProviderFactory();

    static TokenProvider get(@Nonnull ServletContext context) {
        Objects.requireNonNull(context);
        return (TokenProvider) context.getAttribute(ATTRIBUTE_TOKEN_PROVIDER);
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOG.trace("initializing context");

        ServletContext context = event.getServletContext();

        // set TokenProvider
        if (context.getAttribute(ATTRIBUTE_TOKEN_PROVIDER) == null) {
            try {
                TokenProvider provider = FACTORY.newInstance();
                context.setAttribute(ATTRIBUTE_TOKEN_PROVIDER, provider);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            LOG.debug("token provider exists", context.getAttribute(ATTRIBUTE_TOKEN_PROVIDER));
        }

        // set character encoding
        context.setResponseCharacterEncoding(StandardCharsets.UTF_8.name());
    }
}
