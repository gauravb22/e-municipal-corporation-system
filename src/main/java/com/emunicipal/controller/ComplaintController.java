package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.emunicipal.entity.User;
import com.emunicipal.entity.Complaint;
import com.emunicipal.entity.StaffUser;
import com.emunicipal.service.ComplaintService;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.repository.ComplaintRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;

@Controller
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @GetMapping("/raise-complaint")
    public String raiseComplaint(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        return "raise-complaint";
    }

    @GetMapping("/complaint-form")
    public String complaintForm(@RequestParam("type") String type, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("complaintType", type);
        return "complaint-form";
    }

    @PostMapping("/submit-complaint")
    public String submitComplaint(
            @RequestParam("type") String type,
            @RequestParam("location") String location,
            @RequestParam("houseNo") String houseNo,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "wardNo", required = false) String wardNo,
            @RequestParam(value = "wardZone", required = false) String wardZone,
            @RequestParam("photoTimestamp") String photoTimestamp,
            @RequestParam("photoLocation") String photoLocation,
            @RequestParam("photoLatitude") String photoLatitude,
            @RequestParam("photoLongitude") String photoLongitude,
            @RequestParam(value = "photoBase64", required = false) String photoBase64,
            HttpSession session, Model model) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Create and save complaint
        Complaint complaint = new Complaint(
            user.getId(),
            type,
            location,
            houseNo,
            description,
            photoTimestamp,
            photoLocation,
            photoLatitude,
            photoLongitude,
            photoBase64
        );

        Integer resolvedWardNo = null;
        if (wardNo != null && !wardNo.isBlank()) {
            try {
                resolvedWardNo = Integer.valueOf(wardNo.trim());
            } catch (NumberFormatException ex) {
                resolvedWardNo = null;
            }
        }
        if (resolvedWardNo == null) {
            resolvedWardNo = user.getWardNo();
        }
        String resolvedWardZone = (wardZone != null && !wardZone.isBlank()) ? wardZone.trim() : user.getWardZone();

        complaint.setWardNo(resolvedWardNo);
        complaint.setWardZone(resolvedWardZone);
        
        complaintService.saveComplaint(complaint);
        
        System.out.println("================== COMPLAINT SUBMITTED ==================");
        System.out.println("Complaint Type: " + type);
        System.out.println("Location: " + location);
        System.out.println("House Number: " + houseNo);
        System.out.println("Description: " + (description != null ? description : "Not provided"));
        System.out.println("User: " + user.getFullName() + " (" + user.getPhone() + ")");
        System.out.println("Photo Timestamp: " + photoTimestamp);
        System.out.println("Location: " + photoLocation);
        System.out.println("========================================================");
        
        model.addAttribute("success", "Complaint submitted successfully!");
        model.addAttribute("user", user);
        return "redirect:/complaint-status";
    }

    @GetMapping("/complaint-status")
    public String complaintStatus(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Fetch fresh user from DB to ensure we have latest data
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", freshUser);
        
        List<Complaint> complaints = complaintService.getUserComplaints(freshUser.getId());
        
        model.addAttribute("user", freshUser);
        model.addAttribute("complaints", complaints);
        model.addAttribute("complaintService", complaintService);
        return "complaint-status";
    }

    @PostMapping("/escalate-complaint/{id}")
    public String escalateComplaint(@PathVariable("id") Long complaintId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        complaintService.escalateComplaint(complaintId);
        return "redirect:/complaint-status";
    }

    @PostMapping("/submit-feedback/{id}")
    public String submitFeedback(
            @PathVariable("id") Long complaintId,
            @RequestParam("solved") Boolean solved,
            @RequestParam("description") String description,
            @RequestParam(value = "solvedBy", required = false) String solvedBy,
            @RequestParam("rating") Integer rating,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        complaintService.submitFeedback(complaintId, solved, description, rating, solvedBy);
        
        System.out.println("================== FEEDBACK SUBMITTED ==================");
        System.out.println("Complaint ID: " + complaintId);
        System.out.println("Solved: " + solved);
        System.out.println("Rating: " + rating + " stars");
        System.out.println("Description: " + description);
        System.out.println("========================================================");
        
        return "redirect:/complaint-status";
    }

    @GetMapping("/ward-complaints")
    public String wardComplaints(HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        Integer wardNo = staffUser.getWardNo();
        if (wardNo == null) {
            return "ward-complaints";
        }

        List<Complaint> complaints = complaintRepository.findByWardNoOrderByCreatedAtDesc(wardNo);

        List<Complaint> newComplaints = new ArrayList<>();
        List<Complaint> approvedComplaints = new ArrayList<>();
        List<Complaint> pendingComplaints = new ArrayList<>();
        List<Complaint> completedComplaints = new ArrayList<>();

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        for (Complaint c : complaints) {
            String status = c.getStatus() == null ? "" : c.getStatus();
            if ("solved".equalsIgnoreCase(status)) {
                completedComplaints.add(c);
                continue;
            }

            LocalDateTime created = c.getCreatedAt();
            if ("approved".equalsIgnoreCase(status)) {
                approvedComplaints.add(c);
            } else if ("pending".equalsIgnoreCase(status) && created != null && created.isAfter(cutoff)) {
                newComplaints.add(c);
            } else {
                pendingComplaints.add(c);
            }
        }

        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (Complaint c : complaints) {
            String type = c.getComplaintType() == null ? "Other" : c.getComplaintType();
            typeCounts.put(type, typeCounts.getOrDefault(type, 0L) + 1L);
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("newComplaints", newComplaints);
        model.addAttribute("approvedComplaints", approvedComplaints);
        model.addAttribute("pendingComplaints", pendingComplaints);
        model.addAttribute("completedComplaints", completedComplaints);
        model.addAttribute("typeCounts", typeCounts);
        return "ward-complaints";
    }

    @GetMapping("/ward-complaints/new")
    public String wardComplaintsNew(HttpSession session, Model model) {
        return wardComplaintsByType("new", session, model);
    }

    @GetMapping("/ward-complaints/approved")
    public String wardComplaintsApproved(HttpSession session, Model model) {
        return wardComplaintsByType("approved", session, model);
    }

    @GetMapping("/ward-complaints/pending")
    public String wardComplaintsPending(HttpSession session, Model model) {
        return wardComplaintsByType("pending", session, model);
    }

    @GetMapping("/ward-complaints/completed")
    public String wardComplaintsCompleted(HttpSession session, Model model) {
        return wardComplaintsByType("completed", session, model);
    }

    @GetMapping("/ward-complaints/{id:\\d+}")
    public String wardComplaintDetail(@PathVariable("id") Long complaintId,
                                      HttpSession session,
                                      Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        Integer wardNo = staffUser.getWardNo();
        if (wardNo == null) {
            return "redirect:/ward-complaints";
        }

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null || !wardNo.equals(complaint.getWardNo())) {
            return "redirect:/ward-complaints";
        }

        User user = userRepository.findById(complaint.getUserId()).orElse(null);

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("complaint", complaint);
        model.addAttribute("citizen", user);
        return "ward-complaint-detail";
    }

    @PostMapping("/ward-complaints/{id}/status")
    public String updateWardComplaintStatus(@PathVariable("id") Long complaintId,
                                            @RequestParam("status") String status,
                                            HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        Integer wardNo = staffUser.getWardNo();
        if (wardNo == null) {
            return "redirect:/ward-complaints";
        }

        complaintRepository.findById(complaintId).ifPresent(c -> {
            if (wardNo.equals(c.getWardNo())) {
                String normalized = status == null ? "" : status.trim().toLowerCase();
                switch (normalized) {
                    case "approved":
                    case "repeated":
                    case "not_ward":
                    case "wrong":
                        c.setStatus(normalized);
                        c.setUpdatedAt(LocalDateTime.now());
                        complaintRepository.save(c);
                        break;
                    default:
                        break;
                }
            }
        });

        return "redirect:/ward-complaints/" + complaintId;
    }

    @PostMapping("/ward-complaints/{id}/complete")
    public String completeWardComplaint(@PathVariable("id") Long complaintId,
                                        @RequestParam("donePhotoTimestamp") String donePhotoTimestamp,
                                        @RequestParam("donePhotoLocation") String donePhotoLocation,
                                        @RequestParam("donePhotoLatitude") String donePhotoLatitude,
                                        @RequestParam("donePhotoLongitude") String donePhotoLongitude,
                                        @RequestParam("donePhotoBase64") String donePhotoBase64,
                                        HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        Integer wardNo = staffUser.getWardNo();
        if (wardNo == null) {
            return "redirect:/ward-complaints";
        }

        if (donePhotoBase64 == null || donePhotoBase64.isBlank()) {
            return "redirect:/ward-complaints/" + complaintId;
        }

        complaintRepository.findById(complaintId).ifPresent(c -> {
            if (wardNo.equals(c.getWardNo()) && "approved".equalsIgnoreCase(c.getStatus())) {
                c.setDonePhotoTimestamp(donePhotoTimestamp);
                c.setDonePhotoLocation(donePhotoLocation);
                c.setDonePhotoLatitude(donePhotoLatitude);
                c.setDonePhotoLongitude(donePhotoLongitude);
                c.setDonePhotoBase64(donePhotoBase64);
                c.setStatus("solved");
                c.setUpdatedAt(LocalDateTime.now());
                complaintRepository.save(c);
            }
        });

        return "redirect:/ward-complaints/" + complaintId;
    }

    private String wardComplaintsByType(String type, HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        Integer wardNo = staffUser.getWardNo();
        if (wardNo == null) {
            return "ward-complaints";
        }

        List<Complaint> complaints = complaintRepository.findByWardNoOrderByCreatedAtDesc(wardNo);
        List<Complaint> result = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        for (Complaint c : complaints) {
            String status = c.getStatus() == null ? "" : c.getStatus();
            LocalDateTime created = c.getCreatedAt();

            if ("new".equals(type)) {
                if ("pending".equalsIgnoreCase(status) && created != null && created.isAfter(cutoff)) {
                    result.add(c);
                }
            } else if ("approved".equals(type)) {
                if ("approved".equalsIgnoreCase(status)) {
                    result.add(c);
                }
            } else if ("completed".equals(type)) {
                if ("solved".equalsIgnoreCase(status)) {
                    result.add(c);
                }
            } else {
                if (!"solved".equalsIgnoreCase(status) &&
                        !"approved".equalsIgnoreCase(status) &&
                        !("pending".equalsIgnoreCase(status) && created != null && created.isAfter(cutoff))) {
                    result.add(c);
                }
            }
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("complaints", result);
        model.addAttribute("feedType", type);
        return "ward-complaints-" + type;
    }
}

