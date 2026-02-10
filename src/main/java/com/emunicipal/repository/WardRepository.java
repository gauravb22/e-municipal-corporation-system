package com.emunicipal.repository;

import com.emunicipal.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WardRepository extends JpaRepository<Ward, Long> {
    Ward findByWardNoAndWardZone(Integer wardNo, String wardZone);
}

