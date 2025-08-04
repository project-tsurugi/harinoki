package com.tsurugidb.harinoki;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.*;
import javax.security.auth.*;
import javax.security.auth.callback.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Issues a new refresh token.
 */
public class IssueEncryptedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final Logger LOG = LoggerFactory.getLogger(IssueEncryptedServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.trace("enter: {}", req.getContextPath());
        String encryptedCredential = req.getHeader("X-Encrypted-Credentials");
        if (encryptedCredential == null || encryptedCredential.isEmpty()) {
            LOG.trace("unauthorized"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication required");
            return;
        }
        TokenProvider tokenProvider = ConfigurationHandler.get(getServletContext());
        String user;
        String password;
        try {
            String credential = tokenProvider.decrypto(encryptedCredential);
            String[] userPassDi = credential.split("\n");
            if (userPassDi.length < 2) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType(Constants.HTTP_CONTENT_TYPE);
                JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "invalid parameter");
            }
            user = userPassDi[0];
            password = userPassDi[1];
            if (userPassDi.length > 2) {  // has 3rd line
                try {
                    if (!userPassDi[2].isEmpty()) {
                        var di = Instant.parse(userPassDi[2]);
                        if (Instant.now().isAfter(di)) {
                            LOG.trace("credential is no longer valid"); //$NON-NLS-1$
                            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
                            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "credential is no longer valid");
                            return;
                        }
                    }
                } catch (DateTimeParseException e) {
                    LOG.trace("invalid due instant format"); //$NON-NLS-1$
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.setContentType(Constants.HTTP_CONTENT_TYPE);
                    JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "invalid due instant format");
                    return;
                }
            }
        } catch (Exception e) {
            LOG.trace("invalid parameter"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "invalid parameter");
            return;
        }

        try {
            req.login(user, password);
        } catch (ServletException e) {  // FIXME why ServletException arise in test
            LOG.trace("authentication failed"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed");
            return;
        }
        String remoteUser = req.getRemoteUser();
        if (remoteUser == null || !user.equals(remoteUser)) {
            LOG.trace("authentication failed"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed");
            return;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writeToken(resp, tokenProvider.issue(user, false));
    }
}
