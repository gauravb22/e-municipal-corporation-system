package com.emunicipal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "complaint_type", nullable = false)
    private String complaintType;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "house_no", nullable = false)
    private String houseNo;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "photo_timestamp", nullable = false)
    private String photoTimestamp;

    @Column(name = "photo_location", nullable = false)
    private String photoLocation;

    @Column(name = "photo_latitude", nullable = false)
    private String photoLatitude;

    @Column(name = "photo_longitude", nullable = false)
    private String photoLongitude;

    @Column(name = "photo_base64", columnDefinition = "CLOB")
    private String photoBase64;

    @Column(name = "status", nullable = false)
    private String status; // pending, solved, escalated

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "feedback_submitted")
    private Boolean feedbackSubmitted = false;

    @Column(name = "feedback_description", length = 500)
    private String feedbackDescription;

    @Column(name = "feedback_rating")
    private Integer feedbackRating; // 1-5 stars

    @Column(name = "feedback_solved")
    private Boolean feedbackSolved; // Yes/No - is complaint solved

    @Column(name = "feedback_submitted_at")
    private LocalDateTime feedbackSubmittedAt;

    // ===== CONSTRUCTORS =====
    public Complaint() {}

    public Complaint(Long userId, String complaintType, String location, String houseNo,
                     String description, String photoTimestamp, String photoLocation,
                     String photoLatitude, String photoLongitude, String photoBase64) {
        this.userId = userId;
        this.complaintType = complaintType;
        this.location = location;
        this.houseNo = houseNo;
        this.description = description;
        this.photoTimestamp = photoTimestamp;
        this.photoLocation = photoLocation;
        this.photoLatitude = photoLatitude;
        this.photoLongitude = photoLongitude;
        this.photoBase64 = photoBase64;
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
        this.feedbackSubmitted = false;
    }

    // ===== GETTERS & SETTERS =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getComplaintType() {
        return complaintType;
    }

    public void setComplaintType(String complaintType) {
        this.complaintType = complaintType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getHouseNo() {
        return houseNo;
    }

    public void setHouseNo(String houseNo) {
        this.houseNo = houseNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoTimestamp() {
        return photoTimestamp;
    }

    public void setPhotoTimestamp(String photoTimestamp) {
        this.photoTimestamp = photoTimestamp;
    }

    public String getPhotoLocation() {
        return photoLocation;
    }

    public void setPhotoLocation(String photoLocation) {
        this.photoLocation = photoLocation;
    }

    public String getPhotoLatitude() {
        return photoLatitude;
    }

    public void setPhotoLatitude(String photoLatitude) {
        this.photoLatitude = photoLatitude;
    }

    public String getPhotoLongitude() {
        return photoLongitude;
    }

    public void setPhotoLongitude(String photoLongitude) {
        this.photoLongitude = photoLongitude;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getFeedbackSubmitted() {
        return feedbackSubmitted;
    }

    public void setFeedbackSubmitted(Boolean feedbackSubmitted) {
        this.feedbackSubmitted = feedbackSubmitted;
    }

    public String getFeedbackDescription() {
        return feedbackDescription;
    }

    public void setFeedbackDescription(String feedbackDescription) {
        this.feedbackDescription = feedbackDescription;
    }

    public Integer getFeedbackRating() {
        return feedbackRating;
    }

    public void setFeedbackRating(Integer feedbackRating) {
        this.feedbackRating = feedbackRating;
    }

    public Boolean getFeedbackSolved() {
        return feedbackSolved;
    }

    public void setFeedbackSolved(Boolean feedbackSolved) {
        this.feedbackSolved = feedbackSolved;
    }

    public LocalDateTime getFeedbackSubmittedAt() {
        return feedbackSubmittedAt;
    }

    public void setFeedbackSubmittedAt(LocalDateTime feedbackSubmittedAt) {
        this.feedbackSubmittedAt = feedbackSubmittedAt;
    }
}
