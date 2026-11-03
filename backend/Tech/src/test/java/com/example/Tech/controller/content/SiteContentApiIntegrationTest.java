package com.example.Tech.controller.content;

import com.example.Tech.entity.content.ContentPage;
import com.example.Tech.repository.content.ContentPageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Static pages and SEO files over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SiteContentApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private ContentPageRepository pageRepository;

    @Test
    void onlyPublishedPagesAreServed() throws Exception {
        savePage("trang-thu", "Trang thử", "<h2>Chào</h2>", true);
        savePage("trang-nhap", "Trang nháp", "<p>Chưa đăng</p>", false);

        JsonNode page = data("/api/v1/pages/trang-thu");
        assertThat(page.get("title").asString()).isEqualTo("Trang thử");
        assertThat(page.get("body").asString()).isEqualTo("<h2>Chào</h2>");

        assertThat(data("/api/v1/pages").toString()).contains("trang-thu").doesNotContain("trang-nhap").doesNotContain("Chào");
        mockMvc.perform(get("/api/v1/pages/trang-nhap")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/pages/khong-co")).andExpect(status().isNotFound());
    }

    @Test
    void seoFilesPointAtThePublicSite() throws Exception {
        String robots = mockMvc.perform(get("/api/v1/seo/robots.txt")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(robots).contains("Disallow: /admin/").contains("Disallow: /api/").contains("Sitemap: http").contains("/sitemap.xml");

        savePage("trang-seo", "Trang SEO", "<p>Nội dung</p>", true);
        String sitemap = mockMvc.perform(get("/api/v1/seo/sitemap.xml")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(sitemap).startsWith("<?xml").contains("<urlset").contains("customer/products.html")
                .contains("customer/page.html?slug=trang-seo");
    }

    private void savePage(String slug, String title, String body, boolean published) {
        ContentPage page = new ContentPage();
        page.setSlug(slug);
        page.setTitle(title);
        page.setBody(body);
        page.setPublished(published);
        pageRepository.saveAndFlush(page);
    }

    private JsonNode data(String url) throws Exception {
        String json = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(json).get("data");
    }
}
