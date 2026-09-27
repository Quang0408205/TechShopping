package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Browser origins allowed to call the API.
 * <ul>
 *   <li>{@code app.cors.allowed-origins}: exact origins, e.g. {@code https://shop.example.com} (prod).</li>
 *   <li>{@code app.cors.allowed-origin-patterns}: Spring origin patterns, e.g. {@code http://localhost:[*]}
 *       for any local port (dev only).</li>
 * </ul>
 * An origin is allowed when it matches either list.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins, List<String> allowedOriginPatterns) {

    public CorsProperties {
        allowedOrigins = clean(allowedOrigins);
        allowedOriginPatterns = clean(allowedOriginPatterns);
    }

    private static List<String> clean(List<String> values) {
        return values == null
                ? List.of()
                : values.stream().filter(StringUtils::hasText).map(String::trim).toList();
    }
}
