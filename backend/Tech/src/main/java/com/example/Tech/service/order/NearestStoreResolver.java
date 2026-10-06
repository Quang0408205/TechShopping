package com.example.Tech.service.order;

import com.example.Tech.entity.store.Store;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Picks the store that handles a home-delivery order from the shipping address text (Phase 7). There is no map
 * service in the project (like payments, it is simulated), so "nearest" means: the address names the store's
 * district and city (score 3), its district only (2) or its city only (1). Both sides are compared without
 * accents, case or punctuation, with whole-word matching ("Quận 1" does not match "Quận 10"), and common short
 * forms are expanded (Q.1 / Q1 → quận 1, TP / Tp. → thành phố, HCM / TPHCM / Sài Gòn → Hồ Chí Minh, HN → Hà Nội).
 * Ties and addresses that match nothing go to the open store with the lowest id, so placement never fails while
 * a store is open; an empty list gives empty.
 */
public final class NearestStoreResolver {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_WORD = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DISTRICT_SHORT = Pattern.compile("\\bq\\s*(\\d+)\\b");
    private static final Pattern WARD_SHORT = Pattern.compile("\\bp\\s*(\\d+)\\b");
    private static final Pattern LEADING_ADMIN_WORD =
            Pattern.compile("^(thanh pho|tinh|quan|huyen|thi xa|thi tran)\\s+(?=\\D)");

    private NearestStoreResolver() {
    }

    public static Optional<Store> resolve(String shippingAddress, List<Store> openStores) {
        String address = " " + normalize(shippingAddress) + " ";
        return openStores.stream()
                .max(Comparator.<Store>comparingInt(store -> score(address, store))
                        .thenComparing(Store::getId, Comparator.reverseOrder()));
    }

    static int score(String normalizedAddress, Store store) {
        boolean district = mentions(normalizedAddress, store.getDistrict());
        boolean city = mentions(normalizedAddress, store.getCity());
        return (district ? 2 : 0) + (city ? 1 : 0);
    }

    private static boolean mentions(String normalizedAddress, String place) {
        String term = stripAdminWord(normalize(place));
        return !term.isEmpty() && normalizedAddress.contains(" " + term + " ");
    }

    /** "quan binh thanh" → "binh thanh", "thanh pho ho chi minh" → "ho chi minh"; "quan 1" stays (a bare number is ambiguous). */
    private static String stripAdminWord(String term) {
        return LEADING_ADMIN_WORD.matcher(term).replaceFirst("");
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String plain = COMBINING_MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        plain = NOT_WORD.matcher(plain).replaceAll(" ").trim();
        plain = DISTRICT_SHORT.matcher(plain).replaceAll("quan $1");
        plain = WARD_SHORT.matcher(plain).replaceAll("phuong $1");
        plain = (" " + plain + " ")
                .replace(" tphcm ", " thanh pho ho chi minh ")
                .replace(" hcm ", " ho chi minh ")
                .replace(" sai gon ", " ho chi minh ")
                .replace(" saigon ", " ho chi minh ")
                .replace(" tp ", " thanh pho ")
                .replace(" hn ", " ha noi ");
        return plain.trim().replaceAll("\\s+", " ");
    }
}
