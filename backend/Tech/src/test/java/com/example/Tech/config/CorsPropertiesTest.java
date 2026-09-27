package com.example.Tech.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    void missingLists_becomeEmpty() {
        CorsProperties properties = new CorsProperties(null, null);

        assertThat(properties.allowedOrigins()).isEmpty();
        assertThat(properties.allowedOriginPatterns()).isEmpty();
    }

    @Test
    void blankEntries_areDroppedAndValuesTrimmed() {
        CorsProperties properties = new CorsProperties(
                Arrays.asList(" https://shop.example.com ", "", null, "  "),
                Arrays.asList("", " http://localhost:[*]", null, "http://127.0.0.1:[*] "));

        assertThat(properties.allowedOrigins()).containsExactly("https://shop.example.com");
        assertThat(properties.allowedOriginPatterns()).containsExactly("http://localhost:[*]", "http://127.0.0.1:[*]");
    }
}
