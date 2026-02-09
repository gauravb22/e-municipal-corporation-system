package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.WardWorkLike;

public interface WardWorkLikeRepository extends JpaRepository<WardWorkLike, Long> {
    long countByWorkId(Long workId);
    boolean existsByWorkIdAndUserId(Long workId, Long userId);
}
