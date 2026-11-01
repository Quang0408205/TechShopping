package com.example.Tech.service.content;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    @Test
    void formattingTagsSurvive() {
        String html = "<h2>Tiêu đề</h2><p>Một <strong>đoạn</strong> <em>văn</em><br>hai dòng</p><ul><li>a</li><li>b</li></ul>";
        assertThat(HtmlSanitizer.clean(html)).isEqualTo(html);
    }

    @Test
    void scriptsAndStylesAreRemovedWithTheirContent() {
        assertThat(HtmlSanitizer.clean("<p>Xin chào</p><script>alert(1)</script><style>p{display:none}</style>"))
                .isEqualTo("<p>Xin chào</p>");
        assertThat(HtmlSanitizer.clean("<SCRIPT SRC=//evil.example/x.js></SCRIPT><p>ok</p>")).isEqualTo("<p>ok</p>");
        assertThat(HtmlSanitizer.clean("<p>a</p><script>alert(1)")).isEqualTo("<p>a</p>");
        assertThat(HtmlSanitizer.clean("<iframe src=\"https://evil.example\"></iframe><svg onload=alert(1)></svg>x")).isEqualTo("x");
    }

    @Test
    void eventAttributesAndUnknownAttributesAreDropped() {
        assertThat(HtmlSanitizer.clean("<p onclick=\"alert(1)\" style=\"x\" class=\"c\">a</p>")).isEqualTo("<p>a</p>");
        assertThat(HtmlSanitizer.clean("<img src=x onerror=alert(1)>")).isEmpty();
        assertThat(HtmlSanitizer.clean("<img src=\"https://cdn.example/a.png\" alt=\"Ảnh\" onerror=\"alert(1)\">"))
                .isEqualTo("<img src=\"https://cdn.example/a.png\" alt=\"Ảnh\" loading=\"lazy\">");
    }

    @Test
    void onlySafeLinksSurvive() {
        assertThat(HtmlSanitizer.clean("<a href=\"https://poy.vn/x?a=1&b=2\">liên kết</a>"))
                .isEqualTo("<a href=\"https://poy.vn/x?a=1&amp;b=2\" target=\"_blank\" rel=\"noopener noreferrer\">liên kết</a>");
        assertThat(HtmlSanitizer.clean("<a href=\"/customer/contact.html\">liên hệ</a>"))
                .isEqualTo("<a href=\"/customer/contact.html\" rel=\"noopener noreferrer\">liên hệ</a>");
        assertThat(HtmlSanitizer.clean("<a href=\"mailto:support@poy.vn\">mail</a>")).contains("mailto:support@poy.vn");
        assertThat(HtmlSanitizer.clean("<a href=\"tel:19000000\">gọi</a>")).contains("tel:19000000");
        for (String bad : new String[]{"javascript:alert(1)", "JaVaScRiPt:alert(1)", " javascript:alert(1)", "java\tscript:alert(1)",
                "data:text/html;base64,PHNjcmlwdD4=", "//evil.example/x", "vbscript:x"}) {
            assertThat(HtmlSanitizer.clean("<a href=\"" + bad + "\">x</a>")).as(bad).isEqualTo("x");
        }
    }

    @Test
    void strayAngleBracketsAndAmpersandsAreShownAsText() {
        assertThat(HtmlSanitizer.clean("1 < 2 & 3 > 2 &amp; ok")).isEqualTo("1 &lt; 2 &amp; 3 &gt; 2 &amp; ok");
        assertThat(HtmlSanitizer.clean("<p>a < b")).isEqualTo("<p>a &lt; b</p>");
    }

    @Test
    void unbalancedTagsAreClosedAndStrayClosersIgnored() {
        assertThat(HtmlSanitizer.clean("<p><strong>đậm")).isEqualTo("<p><strong>đậm</strong></p>");
        assertThat(HtmlSanitizer.clean("a</p></strong>b")).isEqualTo("ab");
        assertThat(HtmlSanitizer.clean("<ul><li>a</ul>")).isEqualTo("<ul><li>a</li></ul>");
    }

    @Test
    void commentsAndUnknownTagsAreStripped() {
        assertThat(HtmlSanitizer.clean("<!-- ẩn --><div><span>chữ</span></div><font color=red>đỏ</font>")).isEqualTo("chữđỏ");
        assertThat(HtmlSanitizer.clean(null)).isEmpty();
    }
}
