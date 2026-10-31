package com.example.Tech.service.mail;

import java.util.Locale;
import java.util.Set;

/**
 * Addresses under the top-level domains reserved for testing (RFC 2606 / 6761): the demo accounts of the Docker
 * sample data use {@code @example.test}. No such domain can exist, so sending only earns the shop's SMTP account
 * bounces; the mail services log these as SKIPPED instead.
 */
public final class ReservedAddresses {

    private static final Set<String> RESERVED_TLDS = Set.of("test", "example", "invalid", "localhost");

    public static final String SKIP_REASON = "Reserved test address (sample account), not sent";

    private ReservedAddresses() {
    }

    public static boolean isReserved(String address) {
        if (address == null) {
            return false;
        }
        int at = address.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        String typed = address.substring(at + 1).trim().toLowerCase(Locale.ROOT);
        String domain = typed.endsWith(".") ? typed.substring(0, typed.length() - 1) : typed;
        int dot = domain.lastIndexOf('.');
        return RESERVED_TLDS.contains(dot < 0 ? domain : domain.substring(dot + 1));
    }
}
