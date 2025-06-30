package com.tsurugidb.harinoki;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpUtil {

    static final String ROLE_NAME = "harinoki-user";

    private final int port;

    HttpUtil(int port) {
        this.port = port;
    }

    Response get(String path) throws URISyntaxException, IOException, InterruptedException {
        var client = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .build();
        var request = HttpRequest.newBuilder()
                .uri(new URI("http", null, "localhost", port, path, null, null))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return analyze(response);
    }

    Response get(String path, String user, String pass)
            throws URISyntaxException, IOException, InterruptedException {
        String credential = Base64.getEncoder()
                .encodeToString(String.format("%s:%s", user, pass)
                        .getBytes(StandardCharsets.UTF_8));
        return submit(path, request -> {
            request.header("Authorization", String.format("Basic %s", credential));
        });
    }

    Response submit(String path, String token)
            throws URISyntaxException, IOException, InterruptedException {
        return submit(path, request -> {
            request.header("Authorization", String.format("Bearer %s", token));
        });
    }

    Response get(String path, String encryptedCredential)
            throws URISyntaxException, IOException, InterruptedException {
        return submit(path, request -> {
                request.header("X-Encrypted-Credentials", encryptedCredential);
        });
    }

    Response submit(String path, Consumer<? super HttpRequest.Builder> patch)
            throws URISyntaxException, IOException, InterruptedException {
        var client = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .build();
        var request = HttpRequest.newBuilder()
                .uri(new URI("http", null, "localhost", port, path, null, null))
                .GET();
        patch.accept(request);
        HttpResponse<String> response = client.send(request.build(), BodyHandlers.ofString());
        return analyze(response);
    }

    static Response analyze(HttpResponse<String> response) throws IOException {
        if (response.headers().firstValue("Content-Type")
                .filter(it -> it.contains("/json"))
                .isPresent()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tree = mapper.readTree(response.body());
            return new Response(
                    response.statusCode(),
                    Optional.ofNullable(toString(tree.get(JsonUtil.FIELD_TYPE)))
                            .map(MessageType::deserialize)
                            .orElse(MessageType.UNKNOWN),
                    toString(tree.get(JsonUtil.FIELD_TOKEN)),
                    toString(tree.get(JsonUtil.FIELD_MESSAGE)),
                    toString(tree.get(JsonUtil.FIELD_KEY_TYPE)),
                    toString(tree.get(JsonUtil.FIELD_KEY_DATA)));
        }
        return new Response(response.statusCode(), null, null, null, null, null);
    }

    private static String toString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
