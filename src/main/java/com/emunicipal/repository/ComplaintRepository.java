package com.emunicipal.repository;

import com.emunicipal.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Complaint> findByUserId(Long userId);
    List<Complaint> findByWardNoOrderByCreatedAtDesc(Integer wardNo);
}
