package com.emunicipal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_history")
public class ComplaintStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    @Column(name = "old_status", length = 30)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    @Column(name = "changed_by_type", length = 20)
    private String changedByType; // USER, STAFF, SYSTEM

    @Column(name = "changed_by_id")
    private Long changedById;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    public Long getId() { return id; }

    public Long getComplaintId() { return complaintId; }

    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getOldStatus() { return oldStatus; }

    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }

    public String getNewStatus() { return newStatus; }

    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getChangedByType() { return changedByType; }

    public void setChangedByType(String changedByType) { this.changedByType = changedByType; }

    public Long getChangedById() { return changedById; }

    public void setChangedById(Long changedById) { this.changedById = changedById; }

    public String getNote() { return note; }

    public void setNote(String note) { this.note = note; }

    public LocalDateTime getChangedAt() { return changedAt; }

    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}

