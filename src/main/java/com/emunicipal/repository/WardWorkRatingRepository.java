package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.WardWorkRating;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WardWorkRatingRepository extends JpaRepository<WardWorkRating, Long> {
    WardWorkRating findByWorkIdAndUserId(Long workId, Long userId);

    @Query("select avg(r.rating) from WardWorkRating r where r.workId = :workId")
    Double getAverageRatingByWorkId(@Param("workId") Long workId);
}
