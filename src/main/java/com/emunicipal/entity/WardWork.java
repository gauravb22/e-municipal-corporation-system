package com.emunicipal.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class WardWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Integer wardNo;

    private String wardZone;

    private String address;

    private LocalDate workDoneDate;

    private String doneBy;

    @Column(name = "image_base64", columnDefinition = "CLOB")
    private String imageBase64;

    private LocalDateTime createdAt;

    private Double rating = 0.0;

    // GETTERS & SETTERS

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public Integer getWardNo() { return wardNo; }

    public void setWardNo(Integer wardNo) { this.wardNo = wardNo; }

    public String getWardZone() { return wardZone; }

    public void setWardZone(String wardZone) { this.wardZone = wardZone; }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }

    public LocalDate getWorkDoneDate() { return workDoneDate; }

    public void setWorkDoneDate(LocalDate workDoneDate) { this.workDoneDate = workDoneDate; }

    public String getDoneBy() { return doneBy; }

    public void setDoneBy(String doneBy) { this.doneBy = doneBy; }

    public String getImageBase64() { return imageBase64; }

    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getRating() { return rating; }

    public void setRating(Double rating) { this.rating = rating; }
}
