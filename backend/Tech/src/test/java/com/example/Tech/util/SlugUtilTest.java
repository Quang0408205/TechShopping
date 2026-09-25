package com.example.Tech.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilTest {

    @Test
    void toSlug_removesVietnameseDiacritics() {
        assertThat(SlugUtil.toSlug("Điện thoại iPhone 18 Pro Max 256GB"))
                .isEqualTo("dien-thoai-iphone-18-pro-max-256gb");
    }

    @Test
    void toSlug_trimsSeparators() {
        assertThat(SlugUtil.toSlug("  --Máy tính bảng!! ")).isEqualTo("may-tinh-bang");
    }

    @Test
    void toSlug_keepsPlusAsWord() {
        assertThat(SlugUtil.toSlug("Samsung Galaxy S26+ 5G 12GB/256GB"))
                .isEqualTo("samsung-galaxy-s26-plus-5g-12gb-256gb");
    }

    @Test
    void resolve_prefersGivenSlug() {
        assertThat(SlugUtil.resolve("custom-slug", "Laptop")).isEqualTo("custom-slug");
        assertThat(SlugUtil.resolve(" ", "Laptop")).isEqualTo("laptop");
    }
}
