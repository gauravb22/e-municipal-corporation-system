package com.emunicipal.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ward_work_ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"work_id", "user_id"})
})
public class WardWorkRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_id", nullable = false)
    private Long workId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer rating;

    public Long getId() { return id; }

    public Long getWorkId() { return workId; }

    public void setWorkId(Long workId) { this.workId = workId; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getRating() { return rating; }

    public void setRating(Integer rating) { this.rating = rating; }
}
