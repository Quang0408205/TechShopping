package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

/**
 * Where uploaded images are stored and how their public URL starts.
 * <ul>
 *   <li>{@code app.upload.dir}: folder on disk (Docker: the {@code uploads_data} volume at /app/uploads).</li>
 *   <li>{@code app.upload.public-base-url}: origin the browser uses to reach this backend, e.g.
 *       {@code http://localhost:8080}; stored image URLs are {@code <public-base-url>/uploads/products/<file>}.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(String dir, String publicBaseUrl) {

    public UploadProperties {
        dir = StringUtils.hasText(dir) ? dir.trim() : "uploads";
        publicBaseUrl = StringUtils.hasText(publicBaseUrl)
                ? publicBaseUrl.trim().replaceAll("/+$", "")
                : "http://localhost:8080";
    }

    /** Absolute upload folder (a relative {@code dir} is resolved against the working directory). */
    public Path rootDirectory() {
        return Path.of(dir).toAbsolutePath().normalize();
    }
}
