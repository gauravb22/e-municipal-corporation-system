package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.emunicipal.entity.User;
import com.emunicipal.entity.Complaint;
import com.emunicipal.entity.StaffUser;
import com.emunicipal.service.ComplaintService;
import com.emunicipal.service.NotificationService;
import com.emunicipal.service.UploadStorageService;
import com.emunicipal.service.WardService;
import com.emunicipal.util.ImageFormatValidator;
import com.emunicipal.entity.Ward;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.repository.ComplaintRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;

import org.springframework.web.multipart.MultipartFile;

@Controller
public class ComplaintController {

    private static final Set<String> NO_PHOTO_REQUIRED_TYPES = Set.of(
            "garbage vehicle not coming",
            "safai karmachari absent",
            "safai karmachari not coming"
    );

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private WardService wardService;

    @Autowired
    private UploadStorageService uploadStorageService;

    @Autowired
    private NotificationService notificationService;

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

        populateComplaintFormModel(model, user, type);
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
            @RequestParam(value = "photoTimestamp", required = false) String photoTimestamp,
            @RequestParam(value = "photoLocation", required = false) String photoLocation,
            @RequestParam(value = "photoLatitude", required = false) String photoLatitude,
            @RequestParam(value = "photoLongitude", required = false) String photoLongitude,
            @RequestParam(value = "photoBase64", required = false) String photoBase64,
            @RequestParam(value = "notComingFromDate", required = false) String notComingFromDate,
            HttpSession session, Model model) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        String normalizedLocation = normalizeField(location);
        if (normalizedLocation == null) {
            return complaintFormWithError(model, user, type, "Please enter complaint location before submitting.");
        }

        String normalizedHouseNo = normalizeField(houseNo);
        if (normalizedHouseNo == null) {
            return complaintFormWithError(model, user, type, "Please enter house number before submitting.");
        }

        boolean requiresNotComingDate = isNoPhotoRequiredComplaintType(type);
        boolean requiresPhoto = !requiresNotComingDate;

        LocalDate parsedNotComingFromDate = null;
        if (requiresNotComingDate) {
            if (notComingFromDate == null || notComingFromDate.isBlank()) {
                return complaintFormWithError(model, user, type, "Please select the not coming from date.");
            }
            try {
                parsedNotComingFromDate = LocalDate.parse(notComingFromDate);
            } catch (DateTimeParseException ex) {
                return complaintFormWithError(model, user, type, "Please select a valid not coming from date.");
            }
        } else if (photoBase64 == null || photoBase64.isBlank()) {
            return complaintFormWithError(model, user, type, "Please capture a complaint photo before submitting.");
        } else if (!ImageFormatValidator.isJpgDataUrl(photoBase64)) {
            return complaintFormWithError(model, user, type, "Only JPG format is allowed for complaint photos.");
        } else if (!hasValidPhotoLocation(photoLocation, photoLatitude, photoLongitude)) {
            return complaintFormWithError(model, user, type, "Photo complaint requires current location. Please enable GPS and capture photo again.");
        }

        String resolvedPhotoTimestamp;
        String resolvedPhotoLocation;
        String resolvedPhotoLatitude;
        String resolvedPhotoLongitude;
        if (requiresPhoto) {
            resolvedPhotoTimestamp = (photoTimestamp != null && !photoTimestamp.isBlank())
                    ? photoTimestamp
                    : LocalDateTime.now().toString();
            resolvedPhotoLocation = photoLocation.trim();
            resolvedPhotoLatitude = photoLatitude.trim();
            resolvedPhotoLongitude = photoLongitude.trim();
        } else {
            resolvedPhotoTimestamp = "Not required for this complaint type";
            resolvedPhotoLocation = "Not required for this complaint type";
            resolvedPhotoLatitude = "0";
            resolvedPhotoLongitude = "0";
            photoBase64 = null;
        }

        Complaint complaint = new Complaint(
            user.getId(),
            type,
            normalizedLocation,
            normalizedHouseNo,
            description,
            resolvedPhotoTimestamp,
            resolvedPhotoLocation,
            resolvedPhotoLatitude,
            resolvedPhotoLongitude,
            photoBase64
        );
        complaint.setNotComingFromDate(parsedNotComingFromDate);

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

        Ward ward = wardService.resolveWard(resolvedWardNo, resolvedWardZone);
        if (ward != null) {
            complaint.setWardId(ward.getId());
            complaint.setWardNo(ward.getWardNo());
            complaint.setWardZone(ward.getWardZone());
        }
        
        Complaint saved = complaintService.saveComplaint(complaint);
        if (photoBase64 != null && !photoBase64.isBlank() && saved.getId() != null) {
            try {
                String photoPath = uploadStorageService.storeComplaintPhotoFromDataUrl(saved.getId(), photoBase64);
                saved.setPhotoPath(photoPath);
                // Keep DB small: store the file path, not the base64 payload
                saved.setPhotoBase64(null);
                complaintRepository.save(saved);
                complaintService.recordImage(
                        saved.getId(),
                        "BEFORE",
                        photoPath,
                        resolvedPhotoTimestamp,
                        resolvedPhotoLocation,
                        resolvedPhotoLatitude,
                        resolvedPhotoLongitude,
                        "USER",
                        saved.getUserId()
                );
            } catch (Exception ex) {
                System.out.println("Failed to store complaint photo for complaint " + saved.getId() + ": " + ex.getMessage());
            }
        }
        
        System.out.println("================== COMPLAINT SUBMITTED ==================");
        System.out.println("Complaint Type: " + type);
        System.out.println("Location: " + normalizedLocation);
        System.out.println("House Number: " + normalizedHouseNo);
        System.out.println("Description: " + (description != null ? description : "Not provided"));
        System.out.println("User: " + user.getFullName() + " (" + user.getPhone() + ")");
        System.out.println("Photo Timestamp: " + photoTimestamp);
        System.out.println("Location: " + photoLocation);
        System.out.println("========================================================");
        
        model.addAttribute("success", "Complaint submitted successfully!");
        model.addAttribute("user", user);
        return "redirect:/dashboard?complaintSubmitted=1";
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
        complaintService.refreshOverdueForComplaints(complaints);

        List<Complaint> newComplaints = new ArrayList<>();
        List<Complaint> approvedComplaints = new ArrayList<>();
        List<Complaint> pendingComplaints = new ArrayList<>();
        List<Complaint> completedComplaints = new ArrayList<>();

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        for (Complaint c : complaints) {
            String status = c.getStatus() == null ? "" : c.getStatus();
            if ("completed".equalsIgnoreCase(status) || "verified".equalsIgnoreCase(status) || "solved".equalsIgnoreCase(status)) {
                completedComplaints.add(c);
                continue;
            }

            LocalDateTime created = c.getCreatedAt();
            if ("approved".equalsIgnoreCase(status)) {
                approvedComplaints.add(c);
            } else if (("submitted".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) && created != null && created.isAfter(cutoff)) {
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

        Map<Long, User> citizenByComplaintId = buildCitizenLookupByComplaintId(complaints);

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("newComplaints", newComplaints);
        model.addAttribute("approvedComplaints", approvedComplaints);
        model.addAttribute("pendingComplaints", pendingComplaints);
        model.addAttribute("completedComplaints", completedComplaints);
        model.addAttribute("typeCounts", typeCounts);
        model.addAttribute("citizenByComplaintId", citizenByComplaintId);
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
        complaintService.refreshOverdueStatus(complaint);

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
                    case "assigned":
                    case "approved":
                    case "in_progress":
                    case "repeated":
                    case "not_ward":
                    case "wrong":
                        String oldStatus = c.getStatus();
                        c.setStatus(normalized);
                        c.setUpdatedAt(LocalDateTime.now());
                        complaintRepository.save(c);
                        complaintService.recordStatusChange(
                                c.getId(),
                                oldStatus,
                                normalized,
                                "STAFF",
                                staffUser.getId(),
                                "Status updated"
                        );
                        if ("approved".equals(normalized) && (oldStatus == null || !"approved".equalsIgnoreCase(oldStatus))) {
                            notificationService.notifyComplaintApproved(c);
                        }
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
                                        @RequestParam(value = "donePhotoTimestamp", required = false) String donePhotoTimestamp,
                                        @RequestParam(value = "donePhotoLocation", required = false) String donePhotoLocation,
                                        @RequestParam(value = "donePhotoLatitude", required = false) String donePhotoLatitude,
                                        @RequestParam(value = "donePhotoLongitude", required = false) String donePhotoLongitude,
                                        @RequestParam("donePhoto") MultipartFile donePhoto,
                                        HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        Integer wardNo = staffUser.getWardNo();
        if (wardNo == null) {
            return "redirect:/ward-complaints";
        }

        if (donePhoto == null || donePhoto.isEmpty()) {
            return "redirect:/ward-complaints/" + complaintId;
        }
        if (!ImageFormatValidator.isJpgMultipartFile(donePhoto)) {
            return "redirect:/ward-complaints/" + complaintId;
        }

        String resolvedDonePhotoTimestamp = (donePhotoTimestamp != null && !donePhotoTimestamp.isBlank())
                ? donePhotoTimestamp
                : LocalDateTime.now().toString();
        String resolvedDonePhotoLocation = (donePhotoLocation != null && !donePhotoLocation.isBlank())
                ? donePhotoLocation
                : "Location not available";
        String resolvedDonePhotoLatitude = (donePhotoLatitude != null && !donePhotoLatitude.isBlank())
                ? donePhotoLatitude
                : "0";
        String resolvedDonePhotoLongitude = (donePhotoLongitude != null && !donePhotoLongitude.isBlank())
                ? donePhotoLongitude
                : "0";

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null) {
            return "redirect:/ward-complaints";
        }

        if (!wardNo.equals(complaint.getWardNo())) {
            return "redirect:/ward-complaints";
        }

        if (!("approved".equalsIgnoreCase(complaint.getStatus()) || "in_progress".equalsIgnoreCase(complaint.getStatus()))) {
            return "redirect:/ward-complaints/" + complaintId;
        }

        String donePhotoPath;
        try {
            donePhotoPath = uploadStorageService.storeComplaintDonePhoto(complaintId, donePhoto);
        } catch (Exception ex) {
            System.out.println("Failed to store completion photo for complaint " + complaintId + ": " + ex.getMessage());
            return "redirect:/ward-complaints/" + complaintId;
        }

        String oldStatus = complaint.getStatus();
        complaint.setDonePhotoTimestamp(resolvedDonePhotoTimestamp);
        complaint.setDonePhotoLocation(resolvedDonePhotoLocation);
        complaint.setDonePhotoLatitude(resolvedDonePhotoLatitude);
        complaint.setDonePhotoLongitude(resolvedDonePhotoLongitude);
        complaint.setDonePhotoPath(donePhotoPath);
        complaint.setDonePhotoBase64(null);
        complaint.setStatus("completed");
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        complaintService.recordImage(
                complaint.getId(),
                "AFTER",
                donePhotoPath,
                resolvedDonePhotoTimestamp,
                resolvedDonePhotoLocation,
                resolvedDonePhotoLatitude,
                resolvedDonePhotoLongitude,
                "STAFF",
                staffUser.getId()
        );
        complaintService.recordStatusChange(
                complaint.getId(),
                oldStatus,
                "completed",
                "STAFF",
                staffUser.getId(),
                "Work completed"
        );
        notificationService.notifyComplaintCompleted(complaint);

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
        complaintService.refreshOverdueForComplaints(complaints);
        List<Complaint> result = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        for (Complaint c : complaints) {
            String status = c.getStatus() == null ? "" : c.getStatus();
            LocalDateTime created = c.getCreatedAt();

            if ("new".equals(type)) {
                if (("submitted".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) && created != null && created.isAfter(cutoff)) {
                    result.add(c);
                }
            } else if ("approved".equals(type)) {
                if ("approved".equalsIgnoreCase(status)) {
                    result.add(c);
                }
            } else if ("completed".equals(type)) {
                if ("completed".equalsIgnoreCase(status) || "verified".equalsIgnoreCase(status) || "solved".equalsIgnoreCase(status)) {
                    result.add(c);
                }
            } else {
                if (!"completed".equalsIgnoreCase(status) &&
                        !"verified".equalsIgnoreCase(status) &&
                        !"solved".equalsIgnoreCase(status) &&
                        !"approved".equalsIgnoreCase(status) &&
                        !(("submitted".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) && created != null && created.isAfter(cutoff))) {
                    result.add(c);
                }
            }
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("complaints", result);
        model.addAttribute("citizenByComplaintId", buildCitizenLookupByComplaintId(result));
        model.addAttribute("feedType", type);
        return "ward-complaints-" + type;
    }

    private Map<Long, User> buildCitizenLookupByComplaintId(List<Complaint> complaints) {
        Map<Long, User> result = new HashMap<>();
        if (complaints == null || complaints.isEmpty()) {
            return result;
        }

        Set<Long> userIds = new HashSet<>();
        for (Complaint complaint : complaints) {
            if (complaint != null && complaint.getUserId() != null) {
                userIds.add(complaint.getUserId());
            }
        }

        Map<Long, User> userById = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userRepository.findAllById(userIds)) {
                if (user != null && user.getId() != null) {
                    userById.put(user.getId(), user);
                }
            }
        }

        for (Complaint complaint : complaints) {
            if (complaint == null || complaint.getId() == null || complaint.getUserId() == null) {
                continue;
            }
            User citizen = userById.get(complaint.getUserId());
            if (citizen != null) {
                result.put(complaint.getId(), citizen);
            }
        }

        return result;
    }

    private boolean isNoPhotoRequiredComplaintType(String complaintType) {
        if (complaintType == null) {
            return false;
        }
        return NO_PHOTO_REQUIRED_TYPES.contains(complaintType.trim().toLowerCase());
    }

    private String normalizeField(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasValidPhotoLocation(String photoLocation, String photoLatitude, String photoLongitude) {
        String normalizedPhotoLocation = normalizeField(photoLocation);
        String normalizedPhotoLatitude = normalizeField(photoLatitude);
        String normalizedPhotoLongitude = normalizeField(photoLongitude);
        if (normalizedPhotoLocation == null
                || normalizedPhotoLatitude == null
                || normalizedPhotoLongitude == null
                || "location not available".equalsIgnoreCase(normalizedPhotoLocation)) {
            return false;
        }

        try {
            double latitude = Double.parseDouble(normalizedPhotoLatitude);
            double longitude = Double.parseDouble(normalizedPhotoLongitude);
            return !(Math.abs(latitude) < 0.000001 && Math.abs(longitude) < 0.000001);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void populateComplaintFormModel(Model model, User user, String complaintType) {
        boolean requiresNotComingDate = isNoPhotoRequiredComplaintType(complaintType);
        model.addAttribute("user", user);
        model.addAttribute("complaintType", complaintType);
        model.addAttribute("requiresNotComingDate", requiresNotComingDate);
        model.addAttribute("requiresPhoto", !requiresNotComingDate);
    }

    private String complaintFormWithError(Model model, User user, String complaintType, String errorMessage) {
        populateComplaintFormModel(model, user, complaintType);
        model.addAttribute("error", errorMessage);
        return "complaint-form";
    }
}
