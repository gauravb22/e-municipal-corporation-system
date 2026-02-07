package com.emunicipal.entity;

import jakarta.persistence.*;

@Entity
public class WardWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Integer wardNo;

    private String doneBy;

    private String imageUrl;

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

    public String getDoneBy() { return doneBy; }

    public void setDoneBy(String doneBy) { this.doneBy = doneBy; }

    public String getImageUrl() { return imageUrl; }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Double getRating() { return rating; }

    public void setRating(Double rating) { this.rating = rating; }
}
