package com.example.Tech.util;

public final class ImageUrlUtil {

    /**
     * Absolute http(s) URL without spaces (surrounding spaces are allowed; the mapper trims them).
     * Uploaded images are stored with their absolute public URL too, so the frontend only ever sees http(s).
     */
    public static final String IMAGE_URL_REGEX = "^\\s*(?i:https?)://\\S+\\s*$";

    public static final int MAX_IMAGE_URL_LENGTH = 2048;

    private ImageUrlUtil() {
    }
}
