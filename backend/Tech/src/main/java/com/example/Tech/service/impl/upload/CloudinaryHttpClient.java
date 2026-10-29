package com.example.Tech.service.impl.upload;

import com.example.Tech.config.CloudinaryProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.upload.CloudinaryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Cloudinary Upload API with signed requests (no SDK). */
@Slf4j
@Component
public class CloudinaryHttpClient implements CloudinaryClient {

    /** Square 400×400 around the face, quality chosen by Cloudinary; applied once at upload ("incoming"). */
    static final String AVATAR_TRANSFORMATION = "c_fill,g_face,h_400,w_400/q_auto:good";

    private final CloudinaryProperties properties;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final RestClient client;

    public CloudinaryHttpClient(CloudinaryProperties properties, JsonMapper jsonMapper, Clock clock) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.client = RestClient.builder().baseUrl(properties.apiBase()).requestFactory(factory).build();
    }

    @Override
    public String upload(byte[] image, String publicId) {
        Map<String, String> signed = new TreeMap<>();
        signed.put("folder", properties.folder());
        signed.put("public_id", publicId);
        signed.put("timestamp", timestamp());
        signed.put("transformation", AVATAR_TRANSFORMATION);
        MultiValueMap<String, Object> form = form(signed);
        form.add("file", new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return publicId;
            }
        });
        JsonNode answer = call("/image/upload", form);
        String url = answer.path("secure_url").asString("");
        if (url.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        }
        return url;
    }

    @Override
    public void destroy(String fullPublicId) {
        Map<String, String> signed = new TreeMap<>();
        signed.put("public_id", fullPublicId);
        signed.put("timestamp", timestamp());
        call("/image/destroy", form(signed));
    }

    /** SHA-1 of the sorted "key=value" pairs joined by "&", followed by the API secret (Cloudinary signed upload). */
    static String sign(Map<String, String> params, String apiSecret) {
        String joined = new TreeMap<>(params).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((joined + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

    private MultiValueMap<String, Object> form(Map<String, String> signed) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        signed.forEach(form::add);
        form.add("api_key", properties.apiKey());
        form.add("signature", sign(signed, properties.apiSecret()));
        return form;
    }

    private JsonNode call(String path, MultiValueMap<String, Object> form) {
        try {
            String body = client.post()
                    .uri("/v1_1/{cloud}" + path, properties.cloudName())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return jsonMapper.readTree(body);
        } catch (RestClientResponseException e) {
            log.warn("Cloudinary {} answered {}: {}", path, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("Cloudinary {} failed: {}", path, e.getMessage());
            throw new BusinessException(ErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        }
    }

    private String timestamp() {
        return Long.toString(clock.instant().getEpochSecond());
    }
}
