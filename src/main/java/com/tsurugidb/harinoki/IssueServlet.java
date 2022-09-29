package com.tsurugidb.harinoki;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Issues a new refresh token.
 */
public class IssueServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final Logger LOG = LoggerFactory.getLogger(IssueServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.trace("enter: {}", req.getContextPath());
        String user = req.getRemoteUser();
        if (user == null) {
            LOG.trace("unauthorized"); //$NON-NLS-1$
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "authentication required");
            return;
        }
        TokenProvider tokens = ConfigurationHandler.get(getServletContext());
        String token = tokens.issue(user, false);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writeToken(resp, token);
    }
}
