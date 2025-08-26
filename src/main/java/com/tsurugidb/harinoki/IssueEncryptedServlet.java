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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Issues a new refresh token.
 */
public class IssueEncryptedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final int MAXIMUM_FORMAT_VERSION = 1;

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
        JsonFactory factory = new JsonFactory();
        String user = "";
        String password = "";
        String expirationDate = null;
        try {
            String jsonText = tokenProvider.decrypto(encryptedCredential);
            JsonParser parser = factory.createParser(jsonText);
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token == JsonToken.VALUE_STRING) {
                    if (parser.getCurrentName().equals("user")) {
                        user = parser.getText();
                    } else if (parser.getCurrentName().equals("password")) {
                        password = parser.getText();
                    } else if (parser.getCurrentName().equals("expiration_date")) {
                        expirationDate = parser.getText();
                    }
                } else if (token == JsonToken.VALUE_NUMBER_INT) {
                    if (parser.getCurrentName().equals("format_version")) {
                        var formatVersion = parser.getIntValue();
                        if (formatVersion > MAXIMUM_FORMAT_VERSION) {
                            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
                            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed due to invalid credential version");
                            return;
                        }
                    }
                }
            }
            if (expirationDate != null) {
                try {
                    var di = Instant.parse(expirationDate);
                    if (Instant.now().isAfter(di)) {
                        LOG.trace("credential is no longer valid"); //$NON-NLS-1$
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        resp.setContentType(Constants.HTTP_CONTENT_TYPE);
                        JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed due to credential expiration");
                        return;
                    }
                } catch (DateTimeParseException e) {
                    LOG.trace("invalid due instant format"); //$NON-NLS-1$
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.setContentType(Constants.HTTP_CONTENT_TYPE);
                    JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed due to invalid expiration date format");
                    return;
                }
            }
        } catch (Exception e) {
            LOG.trace("invalid parameter"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed due to invalid credential");
            return;
        }

        try {
            req.login(user, password);
        } catch (ServletException e) {  // FIXME why ServletException arise in test
            LOG.trace("authentication failed"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed as the combination of user name and password is incorrect");
            return;
        }
        String remoteUser = req.getRemoteUser();
        if (remoteUser == null || !user.equals(remoteUser)) {
            LOG.trace("authentication failed"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication failed as the combination of user name and password is incorrect");
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writeToken(resp, tokenProvider.issue(user, false));
    }
}
