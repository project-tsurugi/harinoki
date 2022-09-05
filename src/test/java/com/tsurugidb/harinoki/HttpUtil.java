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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpUtil {

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
        var client = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .build();
        String credential = Base64.getEncoder()
                .encodeToString(String.format("%s:%s", user, pass)
                        .getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder()
                .uri(new URI("http", null, "localhost", port, path, null, null))
                .header("Authorization", String.format("Basic %s", credential))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        return analyze(response);
    }

    Response submit(String path, String token)
            throws URISyntaxException, IOException, InterruptedException {
        var client = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .build();
        var request = HttpRequest.newBuilder()
                .uri(new URI("http", null, "localhost", port, path, null, null))
                .header("Authorization", String.format("Bearer %s", token))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
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
                    toString(tree.get(JsonUtil.FIELD_TOKEN)),
                    toString(tree.get(JsonUtil.FIELD_MESSAGE)));
        }
        return new Response(response.statusCode(), null, null);
    }

    private static String toString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
