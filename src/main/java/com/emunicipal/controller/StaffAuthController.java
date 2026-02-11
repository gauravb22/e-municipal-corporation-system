package com.emunicipal.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.emunicipal.entity.StaffUser;
import com.emunicipal.repository.StaffUserRepository;
import com.emunicipal.repository.ComplaintRepository;
import com.emunicipal.service.ComplaintService;

import jakarta.servlet.http.HttpSession;
import java.time.Year;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class StaffAuthController {

    @Autowired
    private StaffUserRepository staffUserRepository;
    @Autowired
    private ComplaintRepository complaintRepository;
    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/ward-login")
    public String wardLoginPage(HttpSession session) {
        session.removeAttribute("user");
        return "ward-login";
    }

    @PostMapping("/ward-login")
    @ResponseBody
    public Map<String, Object> wardLogin(@RequestBody Map<String, String> request, HttpSession session) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return Map.of("success", false, "message", "Missing credentials");
        }

        StaffUser staffUser = staffUserRepository.findByUsername(username);

        if (staffUser == null || !password.equals(staffUser.getPassword())) {
            return Map.of("success", false, "message", "Invalid username or password");
        }

        if (!"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return Map.of("success", false, "message", "Not a ward member account");
        }

        session.removeAttribute("user");
        session.setAttribute("staffUser", staffUser);
        session.setAttribute("staffRole", staffUser.getRole());

        return Map.of("success", true, "redirect", "/ward-dashboard");
    }

    @GetMapping("/ward-dashboard")
    public String wardDashboard(HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        Integer wardNo = staffUser.getWardNo();
        if (wardNo != null) {
            complaintService.refreshOverdueForComplaints(complaintRepository.findByWardNoOrderByCreatedAtDesc(wardNo));
            int year = Year.now().getValue();
            LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59, 59);
            long worksCompletedThisYear =
                    complaintRepository.countByWardNoAndStatusAndCreatedAtBetween(wardNo, "completed", start, end)
                    + complaintRepository.countByWardNoAndStatusAndCreatedAtBetween(wardNo, "verified", start, end)
                    + complaintRepository.countByWardNoAndStatusAndCreatedAtBetween(wardNo, "solved", start, end);
            long pendingWorksCount = complaintRepository.countByWardNoAndStatusIn(
                    wardNo, List.of("submitted", "assigned", "approved", "in_progress", "pending", "overdue"));
            long overdueWorksCount = complaintRepository.countByWardNoAndStatusIn(
                    wardNo, List.of("overdue"));
            Double avgRating = complaintRepository.getAverageRatingByWard(wardNo);

            model.addAttribute("worksCompletedThisYear", worksCompletedThisYear);
            model.addAttribute("pendingWorksCount", pendingWorksCount);
            model.addAttribute("overdueWorksCount", overdueWorksCount);
            model.addAttribute("avgRating", avgRating != null ? String.format("%.1f", avgRating) : "0.0");
        }

        model.addAttribute("staffUser", staffUser);
        return "ward-dashboard";
    }

    @GetMapping("/ward-profile")
    public String wardProfile(HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        model.addAttribute("staffUser", staffUser);
        return "ward-profile";
    }

    @PostMapping("/ward-profile")
    public String updateWardProfile(@RequestParam(value = "phone", required = false) String phone,
                                    @RequestParam(value = "photoBase64", required = false) String photoBase64,
                                    @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                    @RequestParam(value = "newPassword", required = false) String newPassword,
                                    @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                    HttpSession session,
                                    Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        if (phone != null && !phone.isBlank()) {
            staffUser.setPhone(phone.trim());
        }

        if (photoBase64 != null && !photoBase64.isBlank()) {
            staffUser.setPhotoBase64(photoBase64);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !currentPassword.equals(staffUser.getPassword())) {
                model.addAttribute("error", "Current password incorrect");
                model.addAttribute("staffUser", staffUser);
                return "ward-profile";
            }
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match");
                model.addAttribute("staffUser", staffUser);
                return "ward-profile";
            }
            staffUser.setPassword(newPassword);
        }

        staffUserRepository.save(staffUser);
        session.setAttribute("staffUser", staffUser);
        model.addAttribute("success", "Profile updated successfully");
        model.addAttribute("staffUser", staffUser);
        return "ward-profile";
    }
}
