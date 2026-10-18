package com.example.Tech.service.product.filter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the free-text specifications of a product into a few filter values per group (plan v5 stage 9):
 * "Snapdragon 8 Elite Gen 5 8 nhân" → chip "Snapdragon 8 series", "5000 mAh" → pin "5000 – 5999 mAh"...
 * Pure functions: the same input always gives the same values, so the whole index can be rebuilt at any time.
 * The groups that exist depend on the category type (phone / tablet, laptop, desktop, monitor, watch).
 */
public final class FilterNormalizer {

    /** One value of one group; {@code order} sorts the values of a group in the filter panel. */
    public record Value(String group, String label, int order) {
    }

    /** What the filter panel shows for a group (key + title). */
    public record Group(String key, String title) {
    }

    public enum Type { PHONE, TABLET, LAPTOP, DESKTOP, MONITOR, WATCH, NONE }

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:[.,]\\d+)?)");
    private static final Pattern MEMORY = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(tb|gb)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HERTZ = Pattern.compile("(\\d{2,3})\\s*hz", Pattern.CASE_INSENSITIVE);
    private static final Pattern MEGAPIXEL = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*mp", Pattern.CASE_INSENSITIVE);
    private static final Pattern MILLIMETRE = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*mm", Pattern.CASE_INSENSITIVE);

    private FilterNormalizer() {
    }

    public static Type typeOf(String categorySlug) {
        String slug = categorySlug == null ? "" : categorySlug.toLowerCase(Locale.ROOT);
        if (slug.contains("dong-ho")) {
            return Type.WATCH;
        }
        if (slug.contains("man-hinh")) {
            return Type.MONITOR;
        }
        if (slug.contains("may-tinh-de-ban")) {
            return Type.DESKTOP;
        }
        if (slug.contains("may-tinh-bang")) {
            return Type.TABLET;
        }
        if (slug.contains("laptop")) {
            return Type.LAPTOP;
        }
        if (slug.contains("dien-thoai")) {
            return Type.PHONE;
        }
        return Type.NONE;
    }

    /** Groups of a category type, in the order the panel shows them (brand, price and needs are handled elsewhere). */
    public static List<Group> groupsOf(Type type) {
        return switch (type) {
            case PHONE, TABLET -> List.of(new Group("ram", "RAM"), new Group("rom", "Bộ nhớ trong"),
                    new Group("chip", "Chip xử lý"), new Group("battery", "Dung lượng pin"),
                    new Group("camera", "Camera sau"), new Group("screen", "Kích thước màn hình"),
                    new Group("refresh", "Tần số quét"));
            case LAPTOP -> List.of(new Group("cpu", "CPU"), new Group("ram", "RAM"), new Group("ssd", "Ổ cứng SSD"),
                    new Group("gpu", "Card đồ hoạ"), new Group("screen", "Kích thước màn hình"));
            case DESKTOP -> List.of(new Group("cpu", "CPU"), new Group("ram", "RAM"), new Group("ssd", "Ổ cứng SSD"),
                    new Group("gpu", "Card đồ hoạ"));
            case MONITOR -> List.of(new Group("size", "Kích thước"), new Group("resolution", "Độ phân giải"),
                    new Group("refresh", "Tần số quét"), new Group("panel", "Tấm nền"));
            case WATCH -> List.of(new Group("gender", "Giới tính"), new Group("face", "Cỡ mặt"),
                    new Group("strap", "Chất liệu dây"));
            default -> List.of();
        };
    }

    /**
     * @param specs    common specifications of the product, name → value (names matched case-insensitively)
     * @param variants (ram, storage) text of each variant of the product
     */
    public static List<Value> normalize(Type type, String productName, Map<String, String> specs, List<String[]> variants) {
        Map<String, String> spec = new LinkedHashMap<>();
        specs.forEach((name, value) -> spec.put(name.trim().toLowerCase(Locale.ROOT), value == null ? "" : value));
        List<Value> out = new ArrayList<>();
        switch (type) {
            case PHONE, TABLET -> {
                ram(out, spec, variants, 16);
                rom(out, variants, spec);
                chip(out, first(spec, "chip xử lý (cpu)", "chip xử lý", "chipset"), type);
                battery(out, spec, type);
                camera(out, first(spec, "độ phân giải camera sau", "camera sau"));
                screenMobile(out, first(spec, "kích thước màn hình"), type);
                refresh(out, first(spec, "tần số quét"), true);
            }
            case LAPTOP, DESKTOP -> {
                cpu(out, first(spec, "công nghệ cpu", "loại cpu", "cpu"), productName);
                ram(out, spec, variants, 32);
                ssd(out, first(spec, "ổ cứng", "ổ cứng ssd"), variants);
                gpu(out, first(spec, "card đồ họa", "card đồ hoạ", "card màn hình"));
                if (type == Type.LAPTOP) {
                    screenLaptop(out, first(spec, "kích thước màn hình"));
                }
            }
            case MONITOR -> {
                sizeMonitor(out, first(spec, "kích thước màn hình"));
                resolution(out, first(spec, "độ phân giải"));
                refresh(out, first(spec, "tần số quét"), false);
                panel(out, first(spec, "tấm nền", "loại màn hình"));
            }
            case WATCH -> {
                gender(out, first(spec, "đối tượng sử dụng", "đối tượng"), productName);
                face(out, first(spec, "đường kính mặt", "kích thước mặt"), productName);
                strap(out, first(spec, "chất liệu dây"));
            }
            default -> {
            }
        }
        return dedupe(out);
    }

    // ---------------------------------------------------------------- groups

    private static void ram(List<Value> out, Map<String, String> spec, List<String[]> variants, int cap) {
        Set<Double> values = new LinkedHashSet<>();
        for (String[] variant : variants) {
            Double gb = memoryGb(variant[0]);
            if (gb != null) {
                values.add(gb);
            }
        }
        if (values.isEmpty()) {
            Double gb = memoryGb(first(spec, "ram", "bộ nhớ ram"));
            if (gb != null) {
                values.add(gb);
            }
        }
        for (double gb : values) {
            if (gb <= 0) {
                continue;
            }
            out.add(gb >= cap ? new Value("ram", cap + "GB trở lên", cap) : new Value("ram", trim(gb) + "GB", (int) gb));
        }
    }

    private static void rom(List<Value> out, List<String[]> variants, Map<String, String> spec) {
        Set<Double> values = new LinkedHashSet<>();
        for (String[] variant : variants) {
            Double gb = memoryGb(variant[1]);
            if (gb != null) {
                values.add(gb);
            }
        }
        if (values.isEmpty()) {
            Double gb = memoryGb(first(spec, "dung lượng lưu trữ", "bộ nhớ trong"));
            if (gb != null) {
                values.add(gb);
            }
        }
        for (double gb : values) {
            if (gb >= 16) {
                out.add(new Value("rom", memoryLabel(gb), (int) gb));
            }
        }
    }

    private static void ssd(List<Value> out, String text, List<String[]> variants) {
        Double gb = memoryGb(text);
        if (gb == null) {
            for (String[] variant : variants) {
                gb = memoryGb(variant[1]);
                if (gb != null) {
                    break;
                }
            }
        }
        if (gb == null) {
            return;
        }
        if (gb < 256) {
            out.add(new Value("ssd", "Dưới 256GB", 0));
        } else if (gb >= 2048) {
            out.add(new Value("ssd", "Từ 2TB", 2048));
        } else {
            out.add(new Value("ssd", memoryLabel(gb), (int) gb.doubleValue()));
        }
    }

    private static void chip(List<Value> out, String text, Type type) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.isBlank()) {
            return;
        }
        String label;
        int order;
        Matcher snap = Pattern.compile("snapdragon\\s*(\\d)").matcher(t);
        Matcher dim = Pattern.compile("dimensity\\s*(\\d)").matcher(t);
        if (t.contains("apple m") || t.matches(".*\\bm\\d\\b.*") && t.contains("apple")) {
            label = "Apple M-series";
            order = 1;
        } else if (t.contains("apple") || t.matches("^a\\d{2}.*")) {
            label = "Apple A-series";
            order = 0;
        } else if (snap.find()) {
            int d = Integer.parseInt(snap.group(1));
            label = d >= 8 ? "Snapdragon 8 series" : d == 7 ? "Snapdragon 7 series" : "Snapdragon 6 / 4 series";
            order = d >= 8 ? 2 : d == 7 ? 3 : 4;
        } else if (dim.find()) {
            int d = Integer.parseInt(dim.group(1));
            label = d >= 9 ? "Dimensity 9000 series" : d == 8 ? "Dimensity 8000 series" : "Dimensity 7000 / 6000 series";
            order = d >= 9 ? 5 : d == 8 ? 6 : 7;
        } else if (t.contains("exynos")) {
            label = "Exynos";
            order = 8;
        } else if (t.contains("tensor")) {
            label = "Google Tensor";
            order = 9;
        } else if (t.contains("helio") || t.contains("mediatek")) {
            label = "MediaTek Helio / khác";
            order = 10;
        } else if (t.contains("kirin")) {
            label = "Kirin";
            order = 11;
        } else if (t.contains("unisoc")) {
            label = "Unisoc";
            order = 12;
        } else {
            label = "Khác";
            order = 99;
        }
        out.add(new Value("chip", label, order));
    }

    private static void battery(List<Value> out, Map<String, String> spec, Type type) {
        String text = first(spec, "dung lượng pin");
        Double mah = null;
        Matcher wh = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*wh", Pattern.CASE_INSENSITIVE).matcher(text);
        Matcher m = Pattern.compile("(\\d{3,6})\\s*mah", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            mah = Double.parseDouble(m.group(1));
        } else if (wh.find()) {
            mah = parse(wh.group(1)) / 3.8 * 1000;
        }
        if (mah == null || mah < 500) {
            return;
        }
        if (type == Type.TABLET) {
            out.add(mah < 7000 ? new Value("battery", "Dưới 7000 mAh", 0)
                    : mah < 9000 ? new Value("battery", "7000 – 8999 mAh", 1) : new Value("battery", "Từ 9000 mAh", 2));
        } else {
            out.add(mah < 4000 ? new Value("battery", "Dưới 4000 mAh", 0)
                    : mah < 5000 ? new Value("battery", "4000 – 4999 mAh", 1)
                    : mah < 6000 ? new Value("battery", "5000 – 5999 mAh", 2) : new Value("battery", "Từ 6000 mAh", 3));
        }
    }

    private static void camera(List<Value> out, String text) {
        Matcher m = MEGAPIXEL.matcher(text);
        if (!m.find()) {
            return;
        }
        double mp = parse(m.group(1));
        out.add(mp < 16 ? new Value("camera", "Dưới 16 MP", 0) : mp < 50 ? new Value("camera", "16 – 49 MP", 1)
                : mp < 64 ? new Value("camera", "50 – 63 MP", 2) : new Value("camera", "Từ 64 MP", 3));
    }

    private static void screenMobile(List<Value> out, String text, Type type) {
        Double inch = number(text);
        if (inch == null || inch < 3) {
            return;
        }
        if (type == Type.TABLET) {
            out.add(inch < 9 ? new Value("screen", "Dưới 9 inch", 0) : inch < 11 ? new Value("screen", "9 – 10.9 inch", 1)
                    : inch < 13 ? new Value("screen", "11 – 12.9 inch", 2) : new Value("screen", "Từ 13 inch", 3));
        } else {
            out.add(inch < 6.0 ? new Value("screen", "Dưới 6.0 inch", 0) : inch < 6.5 ? new Value("screen", "6.0 – 6.4 inch", 1)
                    : inch < 6.8 ? new Value("screen", "6.5 – 6.7 inch", 2) : new Value("screen", "Từ 6.8 inch", 3));
        }
    }

    private static void screenLaptop(List<Value> out, String text) {
        Double inch = number(text);
        if (inch == null || inch < 9) {
            return;
        }
        out.add(inch < 14 ? new Value("screen", "13 inch trở xuống", 0) : inch < 15 ? new Value("screen", "14 inch", 1)
                : inch < 16 ? new Value("screen", "15 – 15.9 inch", 2) : new Value("screen", "16 inch trở lên", 3));
    }

    private static void refresh(List<Value> out, String text, boolean mobile) {
        Matcher m = HERTZ.matcher(text);
        int max = 0;
        while (m.find()) {
            max = Math.max(max, Integer.parseInt(m.group(1)));
        }
        if (max == 0) {
            return;
        }
        if (mobile) {
            out.add(max <= 60 ? new Value("refresh", "60Hz", 0) : max <= 90 ? new Value("refresh", "90Hz", 1)
                    : max <= 120 ? new Value("refresh", "120Hz", 2) : new Value("refresh", "144Hz trở lên", 3));
        } else {
            out.add(max <= 60 ? new Value("refresh", "60Hz trở xuống", 0) : max <= 100 ? new Value("refresh", "75 – 100Hz", 1)
                    : max <= 144 ? new Value("refresh", "120 – 144Hz", 2) : max <= 180 ? new Value("refresh", "165 – 180Hz", 3)
                    : new Value("refresh", "Từ 240Hz", 4));
        }
    }

    private static void cpu(List<Value> out, String text, String name) {
        String t = (text.isBlank() ? name : text).toLowerCase(Locale.ROOT);
        Matcher ultra = Pattern.compile("core ultra\\s*(?:x)?(\\d)").matcher(t);
        Matcher core = Pattern.compile("core\\s*(?:i)?(\\d)\\b").matcher(t);
        Matcher ryzen = Pattern.compile("ryzen\\s*(?:ai\\s*)?(?:max\\s*)?(\\d)").matcher(t);
        String label;
        int order;
        if (ultra.find()) {
            label = "Intel Core Ultra " + ultra.group(1);
            order = 20 + Integer.parseInt(ultra.group(1));
        } else if (core.find()) {
            label = "Intel Core i" + core.group(1) + " / Core " + core.group(1);
            order = 10 + Integer.parseInt(core.group(1));
        } else if (t.contains("ryzen ai")) {
            label = "AMD Ryzen AI";
            order = 39;
        } else if (ryzen.find()) {
            label = "AMD Ryzen " + ryzen.group(1);
            order = 30 + Integer.parseInt(ryzen.group(1));
        } else if (t.matches(".*\\bm\\d\\b.*") || t.contains("apple m")) {
            label = "Apple M-series";
            order = 50;
        } else if (t.contains("snapdragon x")) {
            label = "Snapdragon X";
            order = 60;
        } else {
            label = "Khác";
            order = 99;
        }
        out.add(new Value("cpu", label, order));
    }

    private static void gpu(List<Value> out, String text) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.isBlank()) {
            return;
        }
        if (t.contains("rtx")) {
            out.add(new Value("gpu", "NVIDIA GeForce RTX", 2));
        } else if (t.contains("gtx") || t.matches(".*\\bmx\\s?\\d.*")) {
            out.add(new Value("gpu", "NVIDIA GTX / MX", 3));
        } else if (t.contains("radeon rx") || t.matches(".*\\brx\\s?\\d{3,4}.*")) {
            out.add(new Value("gpu", "AMD Radeon RX", 4));
        } else if (t.contains("arc")) {
            out.add(new Value("gpu", "Intel Arc", 1));
        } else {
            out.add(new Value("gpu", "Đồ hoạ tích hợp", 0));
        }
    }

    private static void sizeMonitor(List<Value> out, String text) {
        Double inch = number(text);
        if (inch == null || inch < 10) {
            return;
        }
        out.add(inch < 24 ? new Value("size", "Dưới 24 inch", 0) : inch < 27 ? new Value("size", "24 – 26.9 inch", 1)
                : inch < 32 ? new Value("size", "27 – 31.9 inch", 2) : new Value("size", "Từ 32 inch", 3));
    }

    private static void resolution(List<Value> out, String text) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.isBlank()) {
            return;
        }
        if (t.contains("8k") || t.contains("7680")) {
            out.add(new Value("resolution", "8K", 6));
        } else if (t.contains("5k") || t.contains("5120")) {
            out.add(new Value("resolution", "5K", 5));
        } else if (t.contains("4k") || t.contains("uhd") || t.contains("3840")) {
            out.add(new Value("resolution", "4K UHD", 4));
        } else if (t.contains("qhd") || t.contains("2k") || t.contains("2560") || t.contains("3440") || t.contains("1440")) {
            out.add(new Value("resolution", "2K QHD", 3));
        } else if (t.contains("full hd") || t.contains("fhd") || t.contains("1920")) {
            out.add(new Value("resolution", "Full HD", 2));
        } else if (t.contains("hd")) {
            out.add(new Value("resolution", "HD", 1));
        } else {
            out.add(new Value("resolution", "Khác", 99));
        }
    }

    private static void panel(List<Value> out, String text) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("oled")) {
            out.add(new Value("panel", "OLED", 3));
        } else if (t.contains("ips")) {
            out.add(new Value("panel", "IPS", 0));
        } else if (t.matches(".*\\bva\\b.*")) {
            out.add(new Value("panel", "VA", 1));
        } else if (t.matches(".*\\btn\\b.*")) {
            out.add(new Value("panel", "TN", 2));
        } else if (!t.isBlank()) {
            out.add(new Value("panel", "Khác", 99));
        }
    }

    private static void gender(List<Value> out, String text, String name) {
        String t = text.toLowerCase(Locale.ROOT);
        String n = (" " + name + " ").toLowerCase(Locale.ROOT);
        String label;
        if (t.contains("trẻ em") || t.contains("bé") || n.contains(" trẻ em ")) {
            label = "Trẻ em";
        } else if (t.contains("cặp") || n.contains(" cặp ")) {
            label = "Cặp đôi";
        } else if (t.contains("unisex") || (t.contains("nam") && t.contains("nữ"))) {
            label = "Unisex";
        } else if (t.contains("nữ") || (t.isBlank() && n.contains(" nữ "))) {
            label = "Nữ";
        } else if (t.contains("nam") || (t.isBlank() && n.contains(" nam "))) {
            label = "Nam";
        } else {
            label = "Unisex";
        }
        out.add(new Value("gender", label, switch (label) {
            case "Nam" -> 0;
            case "Nữ" -> 1;
            case "Unisex" -> 2;
            case "Cặp đôi" -> 3;
            default -> 4;
        }));
    }

    private static void face(List<Value> out, String text, String name) {
        Matcher m = MILLIMETRE.matcher(text);
        if (!m.find()) {
            m = MILLIMETRE.matcher(name);
            if (!m.find()) {
                return;
            }
        }
        double mm = parse(m.group(1));
        if (mm < 15 || mm > 70) {
            return;
        }
        out.add(mm < 36 ? new Value("face", "Dưới 36 mm", 0) : mm < 40 ? new Value("face", "36 – 39 mm", 1)
                : mm < 44 ? new Value("face", "40 – 43 mm", 2) : new Value("face", "Từ 44 mm", 3));
    }

    private static void strap(List<Value> out, String text) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.isBlank()) {
            return;
        }
        boolean any = false;
        if (t.matches(".*\\bda\\b.*")) {
            out.add(new Value("strap", "Da", 0));
            any = true;
        }
        if (t.contains("thép") || t.contains("kim loại") || t.contains("titan") || t.contains("inox") || t.contains("hợp kim")
                || t.contains("vàng") || t.contains("nhôm")) {
            out.add(new Value("strap", "Kim loại", 1));
            any = true;
        }
        if (t.contains("silicone") || t.contains("cao su") || t.contains("tpu") || t.contains("nhựa") || t.contains("fluor")
                || t.contains("resin") || t.contains("silicon")) {
            out.add(new Value("strap", "Silicone / Nhựa", 2));
            any = true;
        }
        if (t.contains("vải") || t.contains("nylon") || t.contains("dệt") || t.contains("polyester")) {
            out.add(new Value("strap", "Vải / Nylon", 3));
            any = true;
        }
        if (t.contains("gốm")) {
            out.add(new Value("strap", "Gốm", 4));
            any = true;
        }
        if (!any) {
            out.add(new Value("strap", "Khác", 99));
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String first(Map<String, String> spec, String... names) {
        for (String name : names) {
            String value = spec.get(name);
            if (value != null && !value.isBlank() && !value.trim().equalsIgnoreCase("không có")) {
                return value;
            }
        }
        return "";
    }

    private static Double memoryGb(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = MEMORY.matcher(text);
        if (m.find()) {
            double n = parse(m.group(1));
            return m.group(2).equalsIgnoreCase("tb") ? n * 1024 : n;
        }
        return null;
    }

    private static String memoryLabel(double gb) {
        return gb >= 1024 ? trim(gb / 1024) + "TB" : trim(gb) + "GB";
    }

    private static Double number(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = NUMBER.matcher(text);
        return m.find() ? parse(m.group(1)) : null;
    }

    private static double parse(String text) {
        return Double.parseDouble(text.replace(',', '.'));
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static List<Value> dedupe(List<Value> values) {
        Set<String> seen = new LinkedHashSet<>();
        List<Value> result = new ArrayList<>();
        for (Value value : values) {
            if (seen.add(value.group() + "|" + value.label())) {
                result.add(value);
            }
        }
        return result;
    }
}
