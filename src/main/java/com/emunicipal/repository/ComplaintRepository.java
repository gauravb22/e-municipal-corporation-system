package com.emunicipal.repository;

import com.emunicipal.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Complaint> findByUserId(Long userId);
    List<Complaint> findByWardNoOrderByCreatedAtDesc(Integer wardNo);
    long countByWardNo(Integer wardNo);
    long countByWardNoAndStatusIn(Integer wardNo, List<String> statuses);
    long countByStatusIn(List<String> statuses);
    long countByStatus(String status);

    long countByWardNoAndCreatedAtBetween(Integer wardNo, LocalDateTime start, LocalDateTime end);
    long countByWardNoAndStatusInAndCreatedAtBetween(Integer wardNo, List<String> statuses, LocalDateTime start, LocalDateTime end);
    long countByWardNoAndStatusAndCreatedAtBetween(Integer wardNo, String status, LocalDateTime start, LocalDateTime end);

    @Query("select avg(c.feedbackRating) from Complaint c where c.wardNo = :wardNo and c.feedbackRating is not null")
    Double getAverageRatingByWard(@Param("wardNo") Integer wardNo);

    @Query("""
            select c from Complaint c
            where c.wardNo = :wardNo
              and c.status in :statuses
              and coalesce(c.updatedAt, c.createdAt) between :start and :end
            order by coalesce(c.updatedAt, c.createdAt) desc
            """)
    List<Complaint> findByWardNoAndStatusInAndCompletedBetween(
            @Param("wardNo") Integer wardNo,
            @Param("statuses") List<String> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
 
