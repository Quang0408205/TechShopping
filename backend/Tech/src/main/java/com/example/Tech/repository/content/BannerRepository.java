package com.example.Tech.repository.content;

import com.example.Tech.entity.content.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Integer> {

    List<Banner> findAllByOrderByDisplayOrderAscIdAsc();

    @Query("""
            select b from Banner b
            where b.active = true and (b.startsAt is null or b.startsAt <= :now) and (b.endsAt is null or b.endsAt > :now)
            order by b.displayOrder asc, b.id asc
            """)
    List<Banner> findShowing(@Param("now") LocalDateTime now);
}
