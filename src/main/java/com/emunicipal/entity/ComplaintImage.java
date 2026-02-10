package com.emunicipal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_images")
public class ComplaintImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    @Column(name = "image_type", nullable = false, length = 20)
    private String imageType; // BEFORE, AFTER

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "photo_timestamp")
    private String photoTimestamp;

    @Column(name = "photo_location")
    private String photoLocation;

    @Column(name = "photo_latitude")
    private String photoLatitude;

    @Column(name = "photo_longitude")
    private String photoLongitude;

    @Column(name = "uploaded_by_type", length = 20)
    private String uploadedByType; // USER, STAFF, SYSTEM

    @Column(name = "uploaded_by_id")
    private Long uploadedById;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }

    public Long getComplaintId() { return complaintId; }

    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getImageType() { return imageType; }

    public void setImageType(String imageType) { this.imageType = imageType; }

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }

    public String getPhotoTimestamp() { return photoTimestamp; }

    public void setPhotoTimestamp(String photoTimestamp) { this.photoTimestamp = photoTimestamp; }

    public String getPhotoLocation() { return photoLocation; }

    public void setPhotoLocation(String photoLocation) { this.photoLocation = photoLocation; }

    public String getPhotoLatitude() { return photoLatitude; }

    public void setPhotoLatitude(String photoLatitude) { this.photoLatitude = photoLatitude; }

    public String getPhotoLongitude() { return photoLongitude; }

    public void setPhotoLongitude(String photoLongitude) { this.photoLongitude = photoLongitude; }

    public String getUploadedByType() { return uploadedByType; }

    public void setUploadedByType(String uploadedByType) { this.uploadedByType = uploadedByType; }

    public Long getUploadedById() { return uploadedById; }

    public void setUploadedById(Long uploadedById) { this.uploadedById = uploadedById; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

