package com.example.Tech.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    /** Lowercase letters/digits separated by single dashes, e.g. "dien-thoai". */
    public static final String SLUG_REGEX = "^[a-z0-9]+(-[a-z0-9]+)*$";

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+)|(-+$)");

    private SlugUtil() {
    }

    /**
     * Converts text (including Vietnamese) to a URL-friendly slug, e.g. "Điện thoại" -> "dien-thoai".
     */
    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }
        // "+" distinguishes models (Galaxy S26 vs S26+), so keep it as a word instead of dropping it
        String text = input.replace('đ', 'd').replace('Đ', 'D').replace("+", " plus ");
        text = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        text = NON_ALPHANUMERIC.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("-");
        return EDGE_DASHES.matcher(text).replaceAll("");
    }

    /**
     * Returns the given slug when present, otherwise a slug generated from the fallback text.
     */
    public static String resolve(String slug, String fallbackText) {
        return slug == null || slug.isBlank() ? toSlug(fallbackText) : slug;
    }
}
