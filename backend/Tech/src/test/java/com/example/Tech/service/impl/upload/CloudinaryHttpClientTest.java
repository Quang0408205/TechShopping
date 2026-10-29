package com.example.Tech.service.impl.upload;

import com.example.Tech.config.CloudinaryProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plan v5 GĐ7: signed Cloudinary calls, checked against a local stand-in server (no real keys needed). */
class CloudinaryHttpClientTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1790000000L), ZoneOffset.UTC);

    private HttpServer server;
    private final List<String> paths = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private int status = 200;
    private String answer = "{\"secure_url\":\"https://res.cloudinary.com/demo/image/upload/v1/techshopping/avatars/abc.png\"}";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            byte[] out = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void signature_isTheOneOfCloudinarysDocumentation() {
        Map<String, String> params = new TreeMap<>(Map.of("timestamp", "1315060510", "public_id", "sample_image"));
        assertThat(CloudinaryHttpClient.sign(params, "abcd")).isEqualTo("b4ad47fb4e25c7bf5f92a20089f9db59bc302313");
    }

    @Test
    void upload_sendsASignedMultipartRequest_withTheSquareCrop_andReturnsTheHttpsUrl() {
        String url = client().upload(new byte[]{1, 2, 3}, "abc");

        assertThat(url).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/techshopping/avatars/abc.png");
        assertThat(paths).containsExactly("/v1_1/demo/image/upload");
        String body = bodies.get(0);
        String expected = CloudinaryHttpClient.sign(Map.of("folder", "techshopping/avatars", "public_id", "abc",
                "timestamp", "1790000000", "transformation", CloudinaryHttpClient.AVATAR_TRANSFORMATION), "secret");
        assertThat(body).contains("name=\"api_key\"", "key123", "name=\"folder\"", "techshopping/avatars",
                "name=\"transformation\"", "c_fill,g_face,h_400,w_400/q_auto:good", "name=\"file\"",
                "name=\"signature\"", expected).doesNotContain("secret\r");
    }

    @Test
    void destroy_signsThePublicId_andARefusalBecomesAStorageError() {
        client().destroy("techshopping/avatars/abc");
        assertThat(paths).containsExactly("/v1_1/demo/image/destroy");
        assertThat(bodies.get(0)).contains("techshopping/avatars/abc",
                CloudinaryHttpClient.sign(Map.of("public_id", "techshopping/avatars/abc", "timestamp", "1790000000"), "secret"));

        status = 401;
        answer = "{\"error\":{\"message\":\"Invalid Signature\"}}";
        assertThatThrownBy(() -> client().upload(new byte[]{1}, "x"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.IMAGE_STORAGE_UNAVAILABLE));
    }

    private CloudinaryHttpClient client() {
        CloudinaryProperties properties = new CloudinaryProperties("demo", "key123", "secret", null,
                "http://127.0.0.1:" + server.getAddress().getPort(), null);
        return new CloudinaryHttpClient(properties, JsonMapper.builder().build(), CLOCK);
    }
}
