package com.example.Tech.service.impl.payment;

import com.example.Tech.config.StripeProperties;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.payment.StripeCheckoutSession;
import com.example.Tech.service.payment.StripeGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Stripe REST API (form-encoded requests, JSON answers) without the Stripe SDK. */
@Slf4j
@Component
public class StripeHttpGateway implements StripeGateway {

    private final StripeProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient client;

    public StripeHttpGateway(StripeProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.client = RestClient.builder()
                .baseUrl(properties.apiBase())
                .requestFactory(factory)
                .build();
    }

    @Override
    public StripeCheckoutSession createSession(NewSession request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("locale", "vi");
        form.add("payment_method_types[0]", "card");
        form.add("client_reference_id", request.orderId().toString());
        form.add("metadata[order_id]", request.orderId().toString());
        form.add("payment_intent_data[metadata][order_id]", request.orderId().toString());
        form.add("line_items[0][quantity]", "1");
        form.add("line_items[0][price_data][currency]", "vnd");
        form.add("line_items[0][price_data][unit_amount]", Long.toString(request.amountVnd()));
        form.add("line_items[0][price_data][product_data][name]", "Đơn hàng " + request.orderCode() + " - POY");
        form.add("success_url", request.successUrl());
        form.add("cancel_url", request.cancelUrl());
        form.add("expires_at", Long.toString(request.expiresAt().getEpochSecond()));
        if (request.customerEmail() != null && !request.customerEmail().isBlank()) {
            form.add("customer_email", request.customerEmail().trim());
        }
        return toSession(call("POST", "/v1/checkout/sessions", form));
    }

    @Override
    public StripeCheckoutSession getSession(String sessionId) {
        return toSession(call("GET", "/v1/checkout/sessions/" + encode(sessionId), null));
    }

    @Override
    public void expireSession(String sessionId) {
        StripeCheckoutSession session = getSession(sessionId);
        if (session.open()) {
            call("POST", "/v1/checkout/sessions/" + encode(sessionId) + "/expire", new LinkedMultiValueMap<>());
        }
    }

    private JsonNode call(String method, String path, MultiValueMap<String, String> form) {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.CARD_PAYMENT_DISABLED);
        }
        try {
            String body = "GET".equals(method)
                    ? client.get().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.secretKey())
                    .retrieve().body(String.class)
                    : client.post().uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.secretKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve().body(String.class);
            return jsonMapper.readTree(body);
        } catch (RestClientResponseException e) {
            log.warn("Stripe {} {} answered {}: {}", method, path, e.getStatusCode().value(),
                    e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("Stripe {} {} failed: {}", method, path, e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    static StripeCheckoutSession toSession(JsonNode node) {
        String reference = text(node, "client_reference_id");
        Long orderId = null;
        if (reference != null && reference.matches("\\d{1,18}")) {
            orderId = Long.valueOf(reference);
        }
        JsonNode amount = node.get("amount_total");
        return new StripeCheckoutSession(text(node, "id"), text(node, "url"), text(node, "status"),
                text(node, "payment_status"), amount == null || amount.isNull() ? null : amount.asLong(),
                text(node, "currency"), paymentIntentId(node.get("payment_intent")), orderId);
    }

    /** payment_intent is an id, or an object when expanded. */
    private static String paymentIntentId(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isObject() ? text(value, "id") : value.asString();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
