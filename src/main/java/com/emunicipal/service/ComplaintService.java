package com.emunicipal.service;

import com.emunicipal.entity.Complaint;
import com.emunicipal.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    // Save a new complaint
    public Complaint saveComplaint(Complaint complaint) {
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setStatus("submitted");
        complaint.setFeedbackSubmitted(false);
        return complaintRepository.save(complaint);
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
            complaint.setStatus("escalated");
            complaint.setUpdatedAt(LocalDateTime.now());
            return complaintRepository.save(complaint);
        }
        return null;
    }

    // Submit feedback
    public Complaint submitFeedback(Long complaintId, Boolean solved, String description, Integer rating, String solvedBy) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isPresent()) {
            Complaint complaint = complaintOpt.get();
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
            
            return complaintRepository.save(complaint);
        }
        return null;
    }

    // Mark complaint as solved (by admin/ward member)
    public Complaint markAsSolved(Long complaintId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isPresent()) {
            Complaint complaint = complaintOpt.get();
            complaint.setStatus("completed");
            complaint.setUpdatedAt(LocalDateTime.now());
            return complaintRepository.save(complaint);
        }
        return null;
    }
}
