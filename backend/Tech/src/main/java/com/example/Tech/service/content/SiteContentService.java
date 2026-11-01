package com.example.Tech.service.content;

import com.example.Tech.dto.request.content.BannerRequest;
import com.example.Tech.dto.request.content.ContentPageRequest;
import com.example.Tech.dto.response.content.BannerResponse;
import com.example.Tech.dto.response.content.ContentPageResponse;
import com.example.Tech.entity.content.Banner;
import com.example.Tech.entity.content.ContentPage;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.content.BannerRepository;
import com.example.Tech.repository.content.ContentPageRepository;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/** Home banners and static pages: read by everyone (what is showing / published), written by an ADMIN. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteContentService {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    /** A path of this site ("customer/products.html?search=iphone"): no scheme and no leading "//", so nothing like javascript: or another host gets in. */
    private static final Pattern SITE_PATH = Pattern.compile("(?!//)[A-Za-z0-9_./?=&%#+-]+");

    private final BannerRepository bannerRepository;
    private final ContentPageRepository pageRepository;
    private final CurrentUserLoader currentUserLoader;
    private final Clock clock;

    public List<BannerResponse> showingBanners() {
        return bannerRepository.findShowing(LocalDateTime.now(clock)).stream().map(SiteContentService::toResponse).toList();
    }

    public List<ContentPageResponse> publishedPages() {
        return pageRepository.findAllByPublishedTrueOrderByTitleAsc().stream()
                .map(page -> toResponse(page, false)).toList();
    }

    public ContentPageResponse publishedPage(String slug) {
        return toResponse(pageRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_PAGE_NOT_FOUND)), true);
    }

    public List<BannerResponse> listBanners(Long userId) {
        requireAdmin(userId);
        return bannerRepository.findAllByOrderByDisplayOrderAscIdAsc().stream().map(SiteContentService::toResponse).toList();
    }

    @Transactional
    public BannerResponse createBanner(Long userId, BannerRequest request) {
        requireAdmin(userId);
        Banner banner = new Banner();
        apply(banner, request);
        Banner saved = bannerRepository.saveAndFlush(banner);
        log.info("User id={} created banner id={}", userId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public BannerResponse updateBanner(Long userId, Integer id, BannerRequest request) {
        requireAdmin(userId);
        Banner banner = findBanner(id);
        apply(banner, request);
        return toResponse(bannerRepository.saveAndFlush(banner));
    }

    @Transactional
    public void deleteBanner(Long userId, Integer id) {
        requireAdmin(userId);
        bannerRepository.delete(findBanner(id));
    }

    public List<ContentPageResponse> listPages(Long userId) {
        requireAdmin(userId);
        return pageRepository.findAllByOrderByTitleAsc().stream().map(page -> toResponse(page, false)).toList();
    }

    public ContentPageResponse getPage(Long userId, String slug) {
        requireAdmin(userId);
        return toResponse(findPage(slug), true);
    }

    /** Creates the page when the slug is new, otherwise replaces it; the body is stored already filtered. */
    @Transactional
    public ContentPageResponse savePage(Long userId, String slug, ContentPageRequest request) {
        requireAdmin(userId);
        if (slug.length() > 80 || !SLUG.matcher(slug).matches()) {
            throw BusinessException.invalidField("slug", "Đường dẫn chỉ gồm chữ thường không dấu, số và dấu gạch ngang");
        }
        String body = HtmlSanitizer.clean(request.body());
        if (body.replaceAll("<[^>]*>", "").isBlank() && !body.contains("<img")) {
            throw BusinessException.invalidField("body", "Nội dung trống hoặc chỉ gồm thẻ không được phép");
        }
        ContentPage page = pageRepository.findById(slug).orElseGet(() -> {
            ContentPage created = new ContentPage();
            created.setSlug(slug);
            return created;
        });
        page.setTitle(request.title().trim());
        page.setBody(body);
        page.setPublished(request.published() == null || request.published());
        page.setUpdatedBy(userId);
        ContentPage saved = pageRepository.saveAndFlush(page);
        log.info("User id={} saved page '{}'", userId, slug);
        return toResponse(saved, true);
    }

    @Transactional
    public void deletePage(Long userId, String slug) {
        requireAdmin(userId);
        pageRepository.delete(findPage(slug));
    }

    private void apply(Banner banner, BannerRequest request) {
        if (!isHttp(request.imageUrl().trim())) {
            throw BusinessException.invalidField("imageUrl", "Ảnh phải là địa chỉ bắt đầu bằng http:// hoặc https://");
        }
        String link = request.linkUrl() == null ? "" : request.linkUrl().trim();
        if (!link.isEmpty() && !isHttp(link) && !SITE_PATH.matcher(link).matches()) {
            throw BusinessException.invalidField("linkUrl", "Liên kết là địa chỉ http(s) hoặc một trang của cửa hàng");
        }
        if (request.startsAt() != null && request.endsAt() != null && !request.endsAt().isAfter(request.startsAt())) {
            throw BusinessException.invalidField("endsAt", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        banner.setTitle(request.title().trim());
        String subtitle = request.subtitle() == null ? "" : request.subtitle().trim();
        banner.setSubtitle(subtitle.isEmpty() ? null : subtitle);
        banner.setImageUrl(request.imageUrl().trim());
        banner.setLinkUrl(link.isEmpty() ? null : link);
        banner.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        banner.setActive(request.active() == null || request.active());
        banner.setStartsAt(request.startsAt());
        banner.setEndsAt(request.endsAt());
    }

    private static boolean isHttp(String url) {
        String lower = url.toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://")) && url.length() > 8 && !url.contains(" ");
    }

    private Banner findBanner(Integer id) {
        return bannerRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.BANNER_NOT_FOUND));
    }

    private ContentPage findPage(String slug) {
        return pageRepository.findById(slug).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_PAGE_NOT_FOUND));
    }

    private void requireAdmin(Long userId) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
    }

    private static BannerResponse toResponse(Banner banner) {
        return new BannerResponse(banner.getId(), banner.getTitle(), banner.getSubtitle(), banner.getImageUrl(),
                banner.getLinkUrl(), banner.getDisplayOrder(), banner.isActive(), banner.getStartsAt(), banner.getEndsAt());
    }

    private static ContentPageResponse toResponse(ContentPage page, boolean withBody) {
        return new ContentPageResponse(page.getSlug(), page.getTitle(), withBody ? page.getBody() : null,
                page.isPublished(), page.getUpdatedAt());
    }
}
