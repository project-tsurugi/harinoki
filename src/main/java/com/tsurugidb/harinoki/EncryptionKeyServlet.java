package com.tsurugidb.harinoki;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Provides the public key needed to encrypt authentication information.
 */
public class EncryptionKeyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static final Logger LOG = LoggerFactory.getLogger(EncryptionKeyServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOG.trace("enter: {}", request.getContextPath());
        var provider = ConfigurationHandler.get(getServletContext());

        var publicKeyPem = provider.publicKeyPem();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writePublicKey(response, publicKeyPem);
    }
}
