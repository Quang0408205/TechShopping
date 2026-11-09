package com.example.Tech.repository.recommendation;

import com.example.Tech.entity.recommendation.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findAllByUserIdOrderByIdAsc(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserPreference p where p.userId = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
