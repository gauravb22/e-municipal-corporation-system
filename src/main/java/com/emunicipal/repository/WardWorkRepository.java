package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.WardWork;
import java.time.LocalDateTime;
import java.util.List;

public interface WardWorkRepository extends JpaRepository<WardWork, Long> {

    List<WardWork> findByWardNo(Integer wardNo);
    List<WardWork> findAllByOrderByCreatedAtDesc();
    List<WardWork> findByWardNoOrderByCreatedAtDesc(Integer wardNo);
    long countByWardNo(Integer wardNo);
    List<WardWork> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    List<WardWork> findByWardNoAndCreatedAtBetweenOrderByCreatedAtDesc(Integer wardNo, LocalDateTime start, LocalDateTime end);

}
