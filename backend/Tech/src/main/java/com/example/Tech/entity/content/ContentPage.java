package com.example.Tech.entity.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** A static page (about, terms, policies) whose body is HTML already filtered by {@code HtmlSanitizer}. */
@Entity
@Table(name = "content_pages")
@Getter
@Setter
@NoArgsConstructor
public class ContentPage {

    @Id
    @Column(name = "slug", length = 80)
    private String slug;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "text", nullable = false)
    private String body;

    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    @Column(name = "updated_by")
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
