package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.WardWork;
import java.util.List;

public interface WardWorkRepository extends JpaRepository<WardWork, Long> {

    List<WardWork> findByWardNo(Integer wardNo);
    List<WardWork> findAllByOrderByCreatedAtDesc();
    List<WardWork> findByWardNoOrderByCreatedAtDesc(Integer wardNo);

}
