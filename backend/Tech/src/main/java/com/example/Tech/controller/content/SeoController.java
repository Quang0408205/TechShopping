package com.example.Tech.controller.content;

import com.example.Tech.config.MailProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * robots.txt and sitemap.xml for the public shop. The web server forwards /robots.txt and /sitemap.xml here; the
 * links use the shop's public address (the same setting as the links in the order mails).
 */
@RestController
@RequestMapping("/api/v1/seo")
@RequiredArgsConstructor
@Tag(name = "SEO", description = "robots.txt and sitemap.xml")
public class SeoController {

    private static final String[] FIXED_PAGES = {"", "customer/products.html", "customer/services.html",
            "customer/stores.html", "customer/contact.html", "customer/recommendation.html"};

    private final MailProperties mailProperties;
    private final JdbcTemplate jdbc;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Crawlers may read the shop, not the staff area, the API or the personal pages")
    public ResponseEntity<String> robots() {
        String site = mailProperties.siteUrl();
        return ResponseEntity.ok("User-agent: *\nDisallow: /admin/\nDisallow: /api/\nDisallow: /auth/\n"
                + "Disallow: /customer/cart.html\nDisallow: /customer/checkout.html\nDisallow: /customer/account.html\n"
                + "Disallow: /customer/orders.html\nDisallow: /customer/order-detail.html\nDisallow: /customer/payment.html\n"
                + "Allow: /\n\nSitemap: " + site + "/sitemap.xml\n");
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Home, listing pages, published static pages and every product on sale")
    public ResponseEntity<String> sitemap() {
        String site = mailProperties.siteUrl();
        List<String> urls = new ArrayList<>();
        for (String page : FIXED_PAGES) {
            urls.add(entry(site + "/" + page, null));
        }
        jdbc.query("select slug, updated_at from content_pages where is_published order by slug",
                rs -> {
                    urls.add(entry(site + "/customer/page.html?slug=" + rs.getString("slug"), rs.getTimestamp("updated_at")));
                });
        jdbc.query("select product_id, updated_at from products where deleted_at is null and is_active order by product_id",
                rs -> {
                    urls.add(entry(site + "/customer/product-detail.html?id=" + rs.getLong("product_id"), rs.getTimestamp("updated_at")));
                });
        return ResponseEntity.ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" + String.join("\n", urls) + "\n</urlset>\n");
    }

    private static String entry(String url, java.sql.Timestamp modified) {
        String last = modified == null ? "" : "<lastmod>" + modified.toLocalDateTime().toLocalDate() + "</lastmod>";
        return "<url><loc>" + url.replace("&", "&amp;") + "</loc>" + last + "</url>";
    }
}
