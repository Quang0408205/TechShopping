package com.example.Tech.service.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StripeSignatureTest {

    private static final String SECRET = "whsec_test_secret";
    private static final String PAYLOAD = "{\"id\":\"evt_1\",\"type\":\"checkout.session.completed\"}";
    private static final long NOW = 1_790_000_000L;

    @Test
    void validSignature_ofStripeFormat_isAccepted() {
        String header = "t=" + NOW + ",v1=" + StripeSignature.sign(NOW + "." + PAYLOAD, SECRET);

        assertThat(StripeSignature.isValid(PAYLOAD, header, SECRET, NOW + 10)).isTrue();
    }

    @Test
    void oneMatchingV1AmongSeveral_isAccepted_v0IsIgnored() {
        String good = StripeSignature.sign(NOW + "." + PAYLOAD, SECRET);
        String header = "t=" + NOW + ",v1=deadbeef,v0=" + good + ",v1=" + good;

        assertThat(StripeSignature.isValid(PAYLOAD, header, SECRET, NOW)).isTrue();
        assertThat(StripeSignature.isValid(PAYLOAD, "t=" + NOW + ",v0=" + good, SECRET, NOW)).isFalse();
    }

    @Test
    void otherSecret_changedBody_oldTimestamp_orBadHeader_areRefused() {
        String header = "t=" + NOW + ",v1=" + StripeSignature.sign(NOW + "." + PAYLOAD, SECRET);

        assertThat(StripeSignature.isValid(PAYLOAD, header, "whsec_other", NOW)).isFalse();
        assertThat(StripeSignature.isValid(PAYLOAD.replace("evt_1", "evt_2"), header, SECRET, NOW)).isFalse();
        assertThat(StripeSignature.isValid(PAYLOAD, header, SECRET, NOW + StripeSignature.TOLERANCE_SECONDS + 1)).isFalse();
        assertThat(StripeSignature.isValid(PAYLOAD, "t=abc,v1=00", SECRET, NOW)).isFalse();
        assertThat(StripeSignature.isValid(PAYLOAD, null, SECRET, NOW)).isFalse();
        assertThat(StripeSignature.isValid(PAYLOAD, header, "", NOW)).isFalse();
    }
}
