package com.example.Tech.service.audit;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void secretsAreHidden_atAnyDepth_otherFieldsStay() {
        String masked = SensitiveData.mask(mapper, """
                {"name":"An","password":"Abc12345","newPassword":"x","nested":{"citizenId":"0123456789","refreshToken":"t","ok":1},
                 "list":[{"cardNumber":"4242","shippingAddress":"1 Lê Lợi"}],"pin":"1234","opinion":"tốt"}""");
        assertThat(masked).contains("\"name\":\"An\"").contains("\"ok\":1").contains("1 Lê Lợi").contains("\"opinion\":\"tốt\"")
                .doesNotContain("Abc12345").doesNotContain("0123456789").doesNotContain("4242").doesNotContain("1234");
        assertThat(masked).contains("\"password\":\"***\"").contains("\"newPassword\":\"***\"").contains("\"pin\":\"***\"");
    }

    @Test
    void emptyOrBrokenBodiesGiveNothing_andALongOneIsCut() {
        assertThat(SensitiveData.mask(mapper, null)).isNull();
        assertThat(SensitiveData.mask(mapper, "  ")).isNull();
        assertThat(SensitiveData.mask(mapper, "{not json")).isNull();
        String longBody = "{\"note\":\"" + "a".repeat(5000) + "\"}";
        assertThat(SensitiveData.mask(mapper, longBody)).hasSize(SensitiveData.MAX_LENGTH + 1).endsWith("…");
    }
}
