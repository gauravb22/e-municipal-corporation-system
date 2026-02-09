package com.emunicipal.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ward_work_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"work_id", "user_id"})
})
public class WardWorkLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_id", nullable = false)
    private Long workId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public Long getId() { return id; }

    public Long getWorkId() { return workId; }

    public void setWorkId(Long workId) { this.workId = workId; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }
}
