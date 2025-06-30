package com.tsurugidb.harinoki;

import java.io.IOException;

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
        String userPassConnected = req.getHeader("X-Encrypted-Credentials");
        if (userPassConnected == null || userPassConnected.isEmpty()) {
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
            String[] userPass = userPassConnected.split("\\.");
            if (userPass.length != 2) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType(Constants.HTTP_CONTENT_TYPE);
                JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "invalid parameter");
            }
            user = tokenProvider.decrypto(userPass[0]);
            password = tokenProvider.decrypto(userPass[1]);
        } catch (Exception e) {
            LOG.trace("Invalid Parameter"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "invalid parameter");
            return;
        }

        try {
            req.login(user, password);
        } catch (ServletException e) {  // FIXME why ServletException arise in test
            LOG.trace("Authentication Failed"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed");
            return;
        }
        String remoteUser = req.getRemoteUser();
        if (remoteUser == null || !user.equals(remoteUser)) {
            LOG.trace("Authentication Failed"); //$NON-NLS-1$
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
