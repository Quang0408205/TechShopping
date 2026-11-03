package com.example.Tech.service.content;

import com.example.Tech.dto.response.content.ContentPageResponse;
import com.example.Tech.entity.content.ContentPage;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.content.ContentPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Static pages (policies, guides) shown to visitors; only published ones are readable. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteContentService {

    private final ContentPageRepository pageRepository;

    public List<ContentPageResponse> publishedPages() {
        return pageRepository.findAllByPublishedTrueOrderByTitleAsc().stream()
                .map(page -> toResponse(page, false)).toList();
    }

    public ContentPageResponse publishedPage(String slug) {
        return toResponse(pageRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_PAGE_NOT_FOUND)), true);
    }

    private static ContentPageResponse toResponse(ContentPage page, boolean withBody) {
        return new ContentPageResponse(page.getSlug(), page.getTitle(), withBody ? page.getBody() : null,
                page.isPublished(), page.getUpdatedAt());
    }
}
