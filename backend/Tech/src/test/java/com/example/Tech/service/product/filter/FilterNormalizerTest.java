package com.example.Tech.service.product.filter;

import com.example.Tech.service.product.filter.FilterNormalizer.Type;
import com.example.Tech.service.product.filter.FilterNormalizer.Value;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterNormalizerTest {

    private static List<String> labels(List<Value> values, String group) {
        return values.stream().filter(v -> v.group().equals(group)).map(Value::label).toList();
    }

    @Test
    void typeOf_followsTheCategorySlug() {
        assertThat(FilterNormalizer.typeOf("dien-thoai")).isEqualTo(Type.PHONE);
        assertThat(FilterNormalizer.typeOf("may-tinh-bang")).isEqualTo(Type.TABLET);
        assertThat(FilterNormalizer.typeOf("may-tinh-de-ban")).isEqualTo(Type.DESKTOP);
        assertThat(FilterNormalizer.typeOf("man-hinh-may-tinh")).isEqualTo(Type.MONITOR);
        assertThat(FilterNormalizer.typeOf("dong-ho-thong-minh")).isEqualTo(Type.WATCH);
        assertThat(FilterNormalizer.typeOf("dong-ho-thoi-trang")).isEqualTo(Type.WATCH);
        assertThat(FilterNormalizer.typeOf("phu-kien")).isEqualTo(Type.NONE);
    }

    @Test
    void phone_bucketsEverySpec() {
        List<Value> values = FilterNormalizer.normalize(Type.PHONE, "Galaxy S", Map.of(
                "Chip xử lý (CPU)", "Qualcomm Snapdragon 8 Elite Gen 5 8 nhân",
                "Dung lượng pin", "5200 mAh",
                "Độ phân giải camera sau", "Chính 50 MP & Phụ 12 MP",
                "Kích thước màn hình", "6.9\"",
                "Tần số quét", "Chính: 120 Hz & Phụ: 60 Hz"),
                List.of(new String[]{"12 GB", "256 GB"}, new String[]{"12 GB", "1 TB"}));
        assertThat(labels(values, "ram")).containsExactly("12GB");
        assertThat(labels(values, "rom")).containsExactly("256GB", "1TB");
        assertThat(labels(values, "chip")).containsExactly("Snapdragon 8 series");
        assertThat(labels(values, "battery")).containsExactly("5000 – 5999 mAh");
        assertThat(labels(values, "camera")).containsExactly("50 – 63 MP");
        assertThat(labels(values, "screen")).containsExactly("Từ 6.8 inch");
        assertThat(labels(values, "refresh")).containsExactly("120Hz");
    }

    @Test
    void phone_appleAndUnknownChips() {
        assertThat(labels(FilterNormalizer.normalize(Type.PHONE, "iPhone", Map.of("Chip xử lý (CPU)", "Apple A18 6 nhân"), List.of()), "chip"))
                .containsExactly("Apple A-series");
        assertThat(labels(FilterNormalizer.normalize(Type.PHONE, "X", Map.of("Chip xử lý (CPU)", "Snapdragon 685 8 nhân"), List.of()), "chip"))
                .containsExactly("Snapdragon 6 / 4 series");
        assertThat(labels(FilterNormalizer.normalize(Type.PHONE, "X", Map.of("Chip xử lý (CPU)", "Unisoc T107"), List.of()), "chip"))
                .containsExactly("Unisoc");
    }

    @Test
    void tablet_batteryInWattHoursIsConverted() {
        List<Value> values = FilterNormalizer.normalize(Type.TABLET, "iPad", Map.of("Dung lượng pin", "28.93 Wh", "Kích thước màn hình", "11\""), List.of());
        assertThat(labels(values, "battery")).containsExactly("7000 – 8999 mAh");
        assertThat(labels(values, "screen")).containsExactly("11 – 12.9 inch");
    }

    @Test
    void laptop_cpuGpuRamSsd() {
        List<Value> values = FilterNormalizer.normalize(Type.LAPTOP, "Laptop", Map.of(
                "Công nghệ CPU", "Intel Core Ultra 7 Arrow Lake",
                "Card đồ họa", "NVIDIA GeForce RTX 5060, 8 GB",
                "Ổ cứng", "1 TB SSD M.2 NVMe PCIe",
                "RAM", "32 GB",
                "Kích thước màn hình", "15.6 inch"), List.of());
        assertThat(labels(values, "cpu")).containsExactly("Intel Core Ultra 7");
        assertThat(labels(values, "gpu")).containsExactly("NVIDIA GeForce RTX");
        assertThat(labels(values, "ssd")).containsExactly("1TB");
        assertThat(labels(values, "ram")).containsExactly("32GB trở lên");
        assertThat(labels(values, "screen")).containsExactly("15 – 15.9 inch");
    }

    @Test
    void laptop_otherCpuFamilies() {
        assertThat(labels(FilterNormalizer.normalize(Type.LAPTOP, "L", Map.of("Công nghệ CPU", "Intel Core i5 Raptor Lake"), List.of()), "cpu"))
                .containsExactly("Intel Core i5 / Core 5");
        assertThat(labels(FilterNormalizer.normalize(Type.LAPTOP, "L", Map.of("Công nghệ CPU", "Intel Core 5 Raptor Lake"), List.of()), "cpu"))
                .containsExactly("Intel Core i5 / Core 5");
        assertThat(labels(FilterNormalizer.normalize(Type.LAPTOP, "L", Map.of("Công nghệ CPU", "AMD Ryzen 7"), List.of()), "cpu"))
                .containsExactly("AMD Ryzen 7");
        assertThat(labels(FilterNormalizer.normalize(Type.LAPTOP, "L", Map.of("Công nghệ CPU", "Apple M4"), List.of()), "cpu"))
                .containsExactly("Apple M-series");
    }

    @Test
    void monitor_sizeResolutionRefreshPanel() {
        List<Value> values = FilterNormalizer.normalize(Type.MONITOR, "M", Map.of(
                "Kích thước màn hình", "27 inch", "Độ phân giải", "QHD (2560 x 1440)", "Tần số quét", "165 Hz", "Tấm nền", "Fast IPS"), List.of());
        assertThat(labels(values, "size")).containsExactly("27 – 31.9 inch");
        assertThat(labels(values, "resolution")).containsExactly("2K QHD");
        assertThat(labels(values, "refresh")).containsExactly("165 – 180Hz");
        assertThat(labels(values, "panel")).containsExactly("IPS");
    }

    @Test
    void watch_genderFaceStrap_withFallbackToTheName() {
        List<Value> values = FilterNormalizer.normalize(Type.WATCH, "CASIO 38.5 mm Nữ LTP", Map.of("Chất liệu dây", "Thép không gỉ"), List.of());
        assertThat(labels(values, "gender")).containsExactly("Nữ");
        assertThat(labels(values, "face")).containsExactly("36 – 39 mm");
        assertThat(labels(values, "strap")).containsExactly("Kim loại");
        assertThat(labels(FilterNormalizer.normalize(Type.WATCH, "W", Map.of("Đối tượng sử dụng", "Trẻ em", "Chất liệu dây", "Silicone, nylon"), List.of()), "strap"))
                .containsExactly("Silicone / Nhựa", "Vải / Nylon");
    }

    @Test
    void missingSpecs_giveNoValues_andAccessoriesNone() {
        assertThat(FilterNormalizer.normalize(Type.PHONE, "X", Map.of(), List.of())).isEmpty();
        assertThat(FilterNormalizer.normalize(Type.NONE, "Ốp lưng", Map.of("RAM", "8 GB"), List.of())).isEmpty();
        assertThat(FilterNormalizer.groupsOf(Type.NONE)).isEmpty();
    }
}
