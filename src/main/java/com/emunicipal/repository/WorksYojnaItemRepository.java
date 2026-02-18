package com.emunicipal.repository;

import com.emunicipal.entity.WorksYojnaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorksYojnaItemRepository extends JpaRepository<WorksYojnaItem, Long> {
    List<WorksYojnaItem> findBySectionTypeOrderByCreatedAtDesc(String sectionType);
}
