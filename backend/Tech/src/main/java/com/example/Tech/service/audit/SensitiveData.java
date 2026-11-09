package com.example.Tech.service.audit;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.regex.Pattern;

/** Hides secrets in the JSON a staff member sent before it is written to the audit log. */
public final class SensitiveData {

    static final int MAX_LENGTH = 2000;

    private static final Pattern SECRET_KEY = Pattern.compile(
            "(?i)(.*(password|passwd|token|secret|credential|citizen|cardnumber|accountnumber|cvv|cvc|authorization).*|otp|pin)");

    private SensitiveData() {
    }

    /** Compact JSON with secret values replaced by "***", cut to {@value #MAX_LENGTH} characters; null when empty or not JSON. */
    public static String mask(JsonMapper mapper, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            String text = mapper.writeValueAsString(maskNode(mapper.readTree(json)));
            return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) + "…" : text;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static JsonNode maskNode(JsonNode node) {
        if (node instanceof ObjectNode object) {
            for (String name : object.propertyNames().stream().toList()) {
                if (SECRET_KEY.matcher(name).matches()) {
                    object.put(name, "***");
                } else {
                    object.set(name, maskNode(object.get(name)));
                }
            }
        } else if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                array.set(i, maskNode(array.get(i)));
            }
        }
        return node;
    }
}
