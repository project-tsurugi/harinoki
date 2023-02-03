package com.tsurugidb.harinoki;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Prints Hello token.
 */
public class HelloServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final Logger LOG = LoggerFactory.getLogger(HelloServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG.trace("enter: {}", req.getContextPath());
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writeOk(resp);
    }
}
