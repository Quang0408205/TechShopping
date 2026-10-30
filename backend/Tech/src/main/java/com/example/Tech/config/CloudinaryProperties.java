package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Account avatars on Cloudinary (cropped square around the face, compressed). Any of the 3 keys empty =
 * avatars stay on the local disk like every other upload. {@code apiBase} is only changed by tests.
 */
@ConfigurationProperties(prefix = "app.upload.cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret, String folder, String apiBase,
                                   String deliveryBase) {

    public CloudinaryProperties {
        cloudName = StringUtils.hasText(cloudName) ? cloudName.trim() : "";
        apiKey = StringUtils.hasText(apiKey) ? apiKey.trim() : "";
        apiSecret = StringUtils.hasText(apiSecret) ? apiSecret.trim() : "";
        folder = StringUtils.hasText(folder) ? folder.trim().replaceAll("^/+|/+$", "") : "techshopping/avatars";
        apiBase = StringUtils.hasText(apiBase) ? apiBase.trim().replaceAll("/+$", "") : "https://api.cloudinary.com";
        deliveryBase = StringUtils.hasText(deliveryBase) ? deliveryBase.trim().replaceAll("/+$", "") : "https://res.cloudinary.com";
    }

    public boolean enabled() {
        return !cloudName.isEmpty() && !apiKey.isEmpty() && !apiSecret.isEmpty();
    }

    /** Every avatar URL of ours starts with this ({@code https://res.cloudinary.com/<cloud>/image/upload/}). */
    public String deliveryPrefix() {
        return deliveryBase + "/" + cloudName + "/image/upload/";
    }
}
