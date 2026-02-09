package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.WardWorkComment;
import java.util.List;

public interface WardWorkCommentRepository extends JpaRepository<WardWorkComment, Long> {
    List<WardWorkComment> findByWorkIdOrderByCreatedAtDesc(Long workId);
}
