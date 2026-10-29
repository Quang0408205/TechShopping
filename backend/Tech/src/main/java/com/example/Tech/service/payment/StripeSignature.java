package com.example.Tech.service.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Checks the Stripe-Signature header ("t=<unix time>,v1=<hex>[,v1=…]"): v1 = HMAC-SHA256 of "t.payload" with the
 * whole whsec_… secret as key, and t at most {@link #TOLERANCE_SECONDS} away from now (replay protection).
 */
public final class StripeSignature {

    public static final long TOLERANCE_SECONDS = 300;

    private StripeSignature() {
    }

    public static boolean isValid(String payload, String header, String secret, long nowEpochSeconds) {
        if (payload == null || header == null || secret == null || secret.isEmpty()) {
            return false;
        }
        Long timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : header.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if ("t".equals(pair[0])) {
                try {
                    timestamp = Long.parseLong(pair[1]);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if ("v1".equals(pair[0])) {
                signatures.add(pair[1]);
            }
        }
        if (timestamp == null || signatures.isEmpty() || Math.abs(nowEpochSeconds - timestamp) > TOLERANCE_SECONDS) {
            return false;
        }
        byte[] expected = sign(timestamp + "." + payload, secret).getBytes(StandardCharsets.US_ASCII);
        return signatures.stream()
                .anyMatch(sent -> MessageDigest.isEqual(expected, sent.getBytes(StandardCharsets.US_ASCII)));
    }

    /** Hex HMAC-SHA256; also used by tests to build a valid header. */
    public static String sign(String signedPayload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
