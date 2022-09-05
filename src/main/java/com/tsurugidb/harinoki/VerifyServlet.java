package com.tsurugidb.harinoki;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Verifies an issued token.
 */
public class VerifyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final Logger LOG = LoggerFactory.getLogger(VerifyServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOG.trace("enter: {}", request.getContextPath());
        var token = TokenUtil.getToken(request, response);
        if (token == null) {
            return;
        }
        var provider = ConfigurationHandler.get(getServletContext());
        if (!TokenUtil.verifyToken(response, provider, token, true, true)) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writeToken(response, token.getToken());
    }
}
