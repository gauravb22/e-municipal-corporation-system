package com.emunicipal.repository;

import com.emunicipal.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Complaint> findByUserId(Long userId);
    List<Complaint> findByWardNoOrderByCreatedAtDesc(Integer wardNo);
    long countByWardNoAndStatusIn(Integer wardNo, List<String> statuses);
    long countByStatusIn(List<String> statuses);
    long countByStatus(String status);

    long countByWardNoAndStatusAndCreatedAtBetween(Integer wardNo, String status, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("select avg(c.feedbackRating) from Complaint c where c.wardNo = :wardNo and c.feedbackRating is not null")
    Double getAverageRatingByWard(@Param("wardNo") Integer wardNo);
}
 
