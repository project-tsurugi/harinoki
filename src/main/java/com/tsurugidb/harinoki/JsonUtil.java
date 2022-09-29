package com.tsurugidb.harinoki;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import jakarta.servlet.http.HttpServletResponse;

final class JsonUtil {

    static final String FIELD_TOKEN = "token"; //$NON-NLS-1$

    static final String FIELD_TYPE = "type"; //$NON-NLS-1$

    static final String FIELD_MESSAGE = "message"; //$NON-NLS-1$

    static final JsonFactory JSON = new JsonFactory()
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

    static void writeToken(@Nonnull HttpServletResponse response, @Nonnull String token) throws IOException {
        Objects.requireNonNull(response);
        Objects.requireNonNull(token);
        try (var json = JSON.createGenerator(response.getWriter())) {
            json.writeStartObject();
            json.writeStringField(FIELD_TYPE, MessageType.OK.serialize());
            json.writeStringField(FIELD_TOKEN, token);
            json.writeNullField(FIELD_MESSAGE);
            json.writeEndObject();
        }
    }

    static void writeMessage(@Nonnull HttpServletResponse response, @Nonnull MessageType type, @Nonnull String message)
            throws IOException {
        Objects.requireNonNull(response);
        Objects.requireNonNull(type);
        Objects.requireNonNull(message);
        try (var json = JSON.createGenerator(response.getWriter())) {
            json.writeStartObject();
            json.writeStringField(FIELD_TYPE, type.serialize());
            json.writeNullField(FIELD_TOKEN);
            json.writeStringField(FIELD_MESSAGE, message);
            json.writeEndObject();
        }
    }

    private JsonUtil() {
        throw new AssertionError();
    }
}
