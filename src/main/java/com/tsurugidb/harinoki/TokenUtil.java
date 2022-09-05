package com.tsurugidb.harinoki;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

final class TokenUtil {

    static final Logger LOG = LoggerFactory.getLogger(VerifyServlet.class);

    static final Pattern PATTERN_BEARER = Pattern.compile("Bearer (?<token>\\S+)");

    static final JsonFactory JSON = new JsonFactory()
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

    static @Nullable DecodedJWT getToken(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null) {
            LOG.trace("auth header is not set"); //$NON-NLS-1$
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(response, "authentication token required");
            return null;
        }
        Matcher matcher = PATTERN_BEARER.matcher(auth);
        if (!matcher.matches()) {
            LOG.trace("auth header is bearer"); //$NON-NLS-1$
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(response, "invalid authentication token");
            return null;
        }
        String token = matcher.group("token"); //$NON-NLS-1$
        try {
            return JWT.decode(token);
        } catch (JWTDecodeException e) {
            LOG.trace("invalid token", e); //$NON-NLS-1$
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(response, "invalid authentication token");
            return null;
        }
    }

    static boolean verifyToken(
            HttpServletResponse response,
            TokenProvider provider,
            DecodedJWT token,
            boolean allowAccess,
            boolean allowRefresh) throws IOException {
        if (allowAccess && isAccessToken(token)) {
            return verifyToken(response, provider.getAccessTokenVerifier(), token);
        }
        if (allowRefresh && isRefreshToken(token)) {
            return verifyToken(response, provider.getRefreshTokenVerifier(), token);
        }
        LOG.trace("invalid subject: {}", token.getSubject()); //$NON-NLS-1$
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(Constants.HTTP_CONTENT_TYPE);
        JsonUtil.writeMessage(response, "invalid token subject");
        return false;
    }

    static boolean isAccessToken(DecodedJWT token) {
        return Objects.equals(token.getSubject(), TokenProvider.SUBJECT_ACCESS_TOKEN);
    }

    static boolean isRefreshToken(DecodedJWT token) {
        return Objects.equals(token.getSubject(), TokenProvider.SUBJECT_REFRESH_TOKEN);
    }

    static String getUserName(DecodedJWT token) {
        return token.getClaim(TokenProvider.CLAIM_USER_NAME).asString();
    }

    private static boolean verifyToken(HttpServletResponse response, JWTVerifier verifier, DecodedJWT token)
            throws IOException {
        try {
            verifier.verify(token);
            return true;
        } catch (TokenExpiredException e) {
            LOG.trace("token expired", e); //$NON-NLS-1$
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(response, "authentication token expired");
        } catch (JWTVerificationException e) {
            LOG.trace("token invalid", e); //$NON-NLS-1$
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(Constants.HTTP_CONTENT_TYPE);
            JsonUtil.writeMessage(response, "invalid authentication token");
        }
        return false;
    }

    private TokenUtil() {
        throw new AssertionError();
    }
}
