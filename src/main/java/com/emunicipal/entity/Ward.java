package com.emunicipal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wards",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ward_no", "ward_zone"}))
public class Ward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ward_no", nullable = false)
    private Integer wardNo;

    @Column(name = "ward_zone", length = 5, nullable = false)
    private String wardZone;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }

    public Integer getWardNo() { return wardNo; }

    public void setWardNo(Integer wardNo) { this.wardNo = wardNo; }

    public String getWardZone() { return wardZone; }

    public void setWardZone(String wardZone) { this.wardZone = wardZone; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

