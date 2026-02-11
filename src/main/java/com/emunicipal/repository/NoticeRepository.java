package com.emunicipal.repository;

import com.emunicipal.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findTop5ByTargetTypeAndActiveTrueOrderByCreatedAtDesc(String targetType);
    List<Notice> findByTargetTypeAndActiveTrueOrderByCreatedAtDesc(String targetType);
}
