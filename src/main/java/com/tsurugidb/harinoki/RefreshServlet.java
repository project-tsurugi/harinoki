package com.tsurugidb.harinoki;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Issues a new access token from a valid refresh token.
 */
public class RefreshServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * The request header name of max expiration time in seconds.
     */
    public static final String KEY_TOKEN_EXPIRATION = "X-Harinoki-Token-Expiration"; //$NON-NLS-1$

    static final Logger LOG = LoggerFactory.getLogger(RefreshServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOG.trace("enter: {}", request.getContextPath());
        var token = TokenUtil.getToken(request, response);
        if (token == null) {
            return;
        }
        var provider = ConfigurationHandler.get(getServletContext());
        if (!TokenUtil.verifyToken(response, provider, token, false, true, true)) {
            return;
        }

        String name = TokenUtil.getUserName(token);
        assert name != null; // already verified
        LOG.trace("user name: {}", name); //$NON-NLS-1$

        String expirationHeader = request.getHeader(KEY_TOKEN_EXPIRATION);
        Duration maxExpiration = null;
        if (expirationHeader != null && !expirationHeader.isBlank()) {
            try {
                var v = Long.parseLong(expirationHeader.trim());
                LOG.trace("max expiration: {}", v);
                maxExpiration = Duration.ofSeconds(v);
            } catch (NumberFormatException e) {
                LOG.warn(MessageFormat.format(
                        "invalid expiration specification: {0}",
                        expirationHeader), e);
            }
        }

        var access = provider.issue(name, true, maxExpiration);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writeToken(response, access);
    }
}
