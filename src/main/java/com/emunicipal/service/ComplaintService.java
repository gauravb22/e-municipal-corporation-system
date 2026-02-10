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

@Service
public class ComplaintService {

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
        return complaintRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get a specific complaint
    public Optional<Complaint> getComplaintById(Long complaintId) {
        return complaintRepository.findById(complaintId);
    }

    // Check if complaint is pending for more than 72 hours
    public boolean isPendingFor72Hours(Complaint complaint) {
        String status = complaint.getStatus() == null ? "" : complaint.getStatus().toLowerCase();
        if (!"submitted".equals(status) && !"pending".equals(status)) {
            return false;
        }
        LocalDateTime createdTime = complaint.getCreatedAt();
        LocalDateTime seventyTwoHoursLater = createdTime.plusHours(72);
        return LocalDateTime.now().isAfter(seventyTwoHoursLater);
    }

    // Calculate time remaining for 72 hours
    public String getTimeRemaining(Complaint complaint) {
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
}
