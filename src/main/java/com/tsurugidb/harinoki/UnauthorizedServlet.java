package com.tsurugidb.harinoki;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Just show unauthorized page.
 */
public class UnauthorizedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writeMessage(resp, MessageType.AUTH_ERROR, "unauthorized request");
    }
}
