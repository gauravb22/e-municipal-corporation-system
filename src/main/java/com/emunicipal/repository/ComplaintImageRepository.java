package com.emunicipal.repository;

import com.emunicipal.entity.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, Long> {
    List<ComplaintImage> findByComplaintIdOrderByCreatedAtDesc(Long complaintId);
}

