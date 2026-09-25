package com.example.Tech.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Rules shared by every place that creates or looks up accounts.
 */
public final class AccountUtil {

    /** Allowed characters of a username (stored lower case). */
    public static final String USERNAME_REGEX = "^[A-Za-z0-9._-]+$";

    public static final int MIN_PASSWORD_LENGTH = 8;

    /** BCrypt only uses the first 72 bytes and Spring Security rejects longer input. */
    public static final int MAX_PASSWORD_BYTES = 72;

    private AccountUtil() {
    }

    /** Email and username are stored trimmed and in lower case, because the UNIQUE constraints are case-sensitive. */
    public static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean exceedsBcryptLimit(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }
}
