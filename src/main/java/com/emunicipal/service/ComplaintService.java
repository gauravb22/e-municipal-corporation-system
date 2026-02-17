package com.emunicipal.service;

import com.emunicipal.entity.Complaint;
import com.emunicipal.entity.ComplaintImage;
import com.emunicipal.entity.ComplaintStatusHistory;
import com.emunicipal.repository.ComplaintRepository;
import com.emunicipal.repository.ComplaintImageRepository;
import com.emunicipal.repository.ComplaintStatusHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ComplaintService {

    private static final Set<String> OVERDUE_BASE_STATUSES = Set.of("submitted", "pending");

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private ComplaintImageRepository complaintImageRepository;

    // Save a new complaint
    public Complaint saveComplaint(Complaint complaint) {
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setStatus("submitted");
        complaint.setFeedbackSubmitted(false);
        Complaint saved = complaintRepository.save(complaint);
        recordStatusChange(saved.getId(), null, "submitted", "USER", saved.getUserId(), "Complaint submitted");
        return saved;
    }

    // Get all complaints for a user
    public List<Complaint> getUserComplaints(Long userId) {
        List<Complaint> complaints = complaintRepository.findByUserIdOrderByCreatedAtDesc(userId);
        refreshOverdueForComplaints(complaints);
        return complaints;
    }

    // Get a specific complaint
    public Optional<Complaint> getComplaintById(Long complaintId) {
        Optional<Complaint> complaint = complaintRepository.findById(complaintId);
        complaint.ifPresent(this::refreshOverdueStatus);
        return complaint;
    }

    // Check if complaint is pending for more than 72 hours
    public boolean isPendingFor72Hours(Complaint complaint) {
        String status = complaint.getStatus() == null ? "" : complaint.getStatus().toLowerCase();
        if (!OVERDUE_BASE_STATUSES.contains(status) && !"overdue".equals(status)) {
            return false;
        }
        if (complaint.getCreatedAt() == null) {
            return false;
        }
        LocalDateTime createdTime = complaint.getCreatedAt();
        LocalDateTime seventyTwoHoursLater = createdTime.plusHours(72);
        return LocalDateTime.now().isAfter(seventyTwoHoursLater);
    }

    // Calculate time remaining for 72 hours
    public String getTimeRemaining(Complaint complaint) {
        if (complaint.getCreatedAt() == null) {
            return "Time data not available";
        }
        LocalDateTime createdTime = complaint.getCreatedAt();
        LocalDateTime seventyTwoHoursLater = createdTime.plusHours(72);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(seventyTwoHoursLater)) {
            return "Overdue - 72 hours exceeded";
        }

        long totalSeconds = java.time.temporal.ChronoUnit.SECONDS.between(now, seventyTwoHoursLater);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        return hours + " hours " + minutes + " minutes";
    }

    public void refreshOverdueForComplaints(List<Complaint> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            return;
        }
        for (Complaint complaint : complaints) {
            refreshOverdueStatus(complaint);
        }
    }

    public void refreshOverdueStatus(Complaint complaint) {
        if (complaint == null) {
            return;
        }
        String status = complaint.getStatus() == null ? "" : complaint.getStatus().toLowerCase();
        if (!OVERDUE_BASE_STATUSES.contains(status)) {
            return;
        }
        if (complaint.getCreatedAt() == null || !LocalDateTime.now().isAfter(complaint.getCreatedAt().plusHours(72))) {
            return;
        }
        String oldStatus = complaint.getStatus();
        complaint.setStatus("overdue");
        complaint.setUpdatedAt(LocalDateTime.now());
        Complaint saved = complaintRepository.save(complaint);
        recordStatusChange(saved.getId(), oldStatus, "overdue", "SYSTEM", null, "Auto-marked overdue after 72 hours");
    }

    // Escalate complaint
    public Complaint escalateComplaint(Long complaintId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isPresent()) {
            Complaint complaint = complaintOpt.get();
            String oldStatus = complaint.getStatus();
            complaint.setStatus("escalated");
            complaint.setUpdatedAt(LocalDateTime.now());
            Complaint saved = complaintRepository.save(complaint);
            recordStatusChange(saved.getId(), oldStatus, "escalated", "SYSTEM", null, "Escalated after 72 hours");
            return saved;
        }
        return null;
    }

    // Submit feedback
    public Complaint submitFeedback(Long complaintId, Boolean solved, String description, Integer rating, String solvedBy) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isPresent()) {
            Complaint complaint = complaintOpt.get();
            String oldStatus = complaint.getStatus();
            complaint.setFeedbackSolved(solved);
            complaint.setFeedbackDescription(description);
            complaint.setFeedbackRating(rating);
            if (solvedBy != null && !solvedBy.isBlank()) {
                complaint.setFeedbackSolvedBy(solvedBy.trim());
            }
            complaint.setFeedbackSubmitted(true);
            complaint.setFeedbackSubmittedAt(LocalDateTime.now());
            
            // If user confirms it's solved, mark as verified
            if (solved) {
                complaint.setStatus("verified");
            } else {
                complaint.setStatus("completed");
            }
            complaint.setUpdatedAt(LocalDateTime.now());
            Complaint saved = complaintRepository.save(complaint);
            recordStatusChange(saved.getId(), oldStatus, saved.getStatus(), "USER", saved.getUserId(), "Feedback submitted");
            return saved;
        }
        return null;
    }

    // Mark complaint as solved (by admin/ward member)
    public Complaint markAsSolved(Long complaintId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isPresent()) {
            Complaint complaint = complaintOpt.get();
            String oldStatus = complaint.getStatus();
            complaint.setStatus("completed");
            complaint.setUpdatedAt(LocalDateTime.now());
            Complaint saved = complaintRepository.save(complaint);
            recordStatusChange(saved.getId(), oldStatus, "completed", "SYSTEM", null, "Marked as solved");
            return saved;
        }
        return null;
    }

    public void recordStatusChange(Long complaintId,
                                   String oldStatus,
                                   String newStatus,
                                   String changedByType,
                                   Long changedById,
                                   String note) {
        if (complaintId == null || newStatus == null) {
            return;
        }
        ComplaintStatusHistory h = new ComplaintStatusHistory();
        h.setComplaintId(complaintId);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus);
        h.setChangedByType(changedByType);
        h.setChangedById(changedById);
        h.setNote(note);
        h.setChangedAt(LocalDateTime.now());
        statusHistoryRepository.save(h);
    }

    public void recordImage(Long complaintId,
                            String imageType,
                            String path,
                            String photoTimestamp,
                            String photoLocation,
                            String photoLatitude,
                            String photoLongitude,
                            String uploadedByType,
                            Long uploadedById) {
        if (complaintId == null || path == null || imageType == null) {
            return;
        }
        ComplaintImage img = new ComplaintImage();
        img.setComplaintId(complaintId);
        img.setImageType(imageType);
        img.setPath(path);
        img.setPhotoTimestamp(photoTimestamp);
        img.setPhotoLocation(photoLocation);
        img.setPhotoLatitude(photoLatitude);
        img.setPhotoLongitude(photoLongitude);
        img.setUploadedByType(uploadedByType);
        img.setUploadedById(uploadedById);
        img.setCreatedAt(LocalDateTime.now());
        complaintImageRepository.save(img);
    }

    public String getStatusBadgeClass(String status) {
        String normalized = status == null ? "submitted" : status.trim().toLowerCase();
        switch (normalized) {
            case "submitted":
            case "pending":
                return "status-submitted";
            case "assigned":
                return "status-assigned";
            case "approved":
                return "status-approved";
            case "in_progress":
                return "status-in-progress";
            case "overdue":
                return "status-overdue";
            case "completed":
            case "solved":
                return "status-completed";
            case "verified":
                return "status-verified";
            case "escalated":
                return "status-escalated";
            case "repeated":
                return "status-repeated";
            case "not_ward":
                return "status-not-ward";
            case "wrong":
                return "status-wrong";
            default:
                return "status-completed";
        }
    }

    public String getStatusLabel(String status) {
        String normalized = status == null ? "submitted" : status.trim().toLowerCase();
        switch (normalized) {
            case "submitted":
            case "pending":
                return "SUBMITTED";
            case "assigned":
                return "ASSIGNED TO WARD";
            case "approved":
                return "APPROVED";
            case "in_progress":
                return "IN PROGRESS";
            case "overdue":
                return "OVERDUE";
            case "completed":
            case "solved":
                return "COMPLETED";
            case "verified":
                return "VERIFIED";
            case "escalated":
                return "ESCALATED";
            case "repeated":
                return "REPEATED";
            case "not_ward":
                return "NOT OUR WARD";
            case "wrong":
                return "WRONG";
            default:
                return normalized.toUpperCase();
        }
    }
}
