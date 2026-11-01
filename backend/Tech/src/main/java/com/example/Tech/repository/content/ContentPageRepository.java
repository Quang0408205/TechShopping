package com.example.Tech.repository.content;

import com.example.Tech.entity.content.ContentPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentPageRepository extends JpaRepository<ContentPage, String> {

    List<ContentPage> findAllByOrderByTitleAsc();

    List<ContentPage> findAllByPublishedTrueOrderByTitleAsc();

    Optional<ContentPage> findBySlugAndPublishedTrue(String slug);
}
