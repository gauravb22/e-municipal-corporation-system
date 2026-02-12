package com.emunicipal.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.emunicipal.entity.StaffUser;
import com.emunicipal.entity.Notice;
import com.emunicipal.repository.StaffUserRepository;
import com.emunicipal.repository.ComplaintRepository;
import com.emunicipal.repository.NoticeRepository;
import com.emunicipal.service.ComplaintService;
import com.emunicipal.service.WardService;

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
    @Autowired
    private NoticeRepository noticeRepository;
    @Autowired
    private WardService wardService;

    @GetMapping("/ward-login")
    public String wardLoginPage(HttpSession session) {
        session.removeAttribute("user");
        return "ward-login";
    }

    @GetMapping("/admin-login")
    public String adminLoginPage(HttpSession session) {
        session.removeAttribute("user");
        return "admin-login";
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

    @PostMapping("/admin-login")
    @ResponseBody
    public Map<String, Object> adminLogin(@RequestBody Map<String, String> request, HttpSession session) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return Map.of("success", false, "message", "Missing credentials");
        }

        StaffUser staffUser = staffUserRepository.findByUsername(username);

        if (staffUser == null || !password.equals(staffUser.getPassword())) {
            return Map.of("success", false, "message", "Invalid username or password");
        }

        if (!"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return Map.of("success", false, "message", "Not an administration account");
        }

        session.removeAttribute("user");
        session.setAttribute("staffUser", staffUser);
        session.setAttribute("staffRole", staffUser.getRole());

        return Map.of("success", true, "redirect", "/admin-dashboard");
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
        model.addAttribute("wardNotices", noticeRepository.findTop5ByTargetTypeAndActiveTrueOrderByCreatedAtDesc("WARD"));
        return "ward-dashboard";
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("totalWardMembers", staffUserRepository.countByRoleIgnoreCase("WARD"));
        return "admin-dashboard";
    }

    @GetMapping("/admin/notices")
    public String adminNotices(HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("citizenNotices", noticeRepository.findByTargetTypeAndActiveTrueOrderByCreatedAtDesc("CITIZEN"));
        model.addAttribute("wardNotices", noticeRepository.findByTargetTypeAndActiveTrueOrderByCreatedAtDesc("WARD"));
        return "admin-notices";
    }

    @GetMapping("/admin/ward-members")
    public String adminWardMembers(HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("totalWardMembers", staffUserRepository.countByRoleIgnoreCase("WARD"));
        model.addAttribute("wardMembers", staffUserRepository.findByRoleIgnoreCaseOrderByWardNoAscWardZoneAscUsernameAsc("WARD"));
        return "admin-ward-members";
    }

    @PostMapping("/admin/ward-members/create")
    public String createWardMember(@RequestParam("fullName") String fullName,
                                   @RequestParam("username") String username,
                                   @RequestParam("password") String password,
                                   @RequestParam("wardNo") Integer wardNo,
                                   @RequestParam("wardZone") String wardZone,
                                   HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return "redirect:/admin/ward-members?error=missing";
        }

        if (wardNo == null || wardZone == null || wardZone.isBlank()) {
            return "redirect:/admin/ward-members?error=ward";
        }

        if (staffUserRepository.findByUsername(username.trim()) != null) {
            return "redirect:/admin/ward-members?error=exists";
        }

        StaffUser wardUser = new StaffUser();
        wardUser.setRole("WARD");
        wardUser.setFullName(fullName != null ? fullName.trim() : null);
        wardUser.setUsername(username.trim());
        wardUser.setPassword(password);
        wardUser.setWardNo(wardNo);
        wardUser.setWardZone(wardZone.trim().toUpperCase());
        var ward = wardService.resolveWard(wardNo, wardZone);
        if (ward != null) {
            wardUser.setWardId(ward.getId());
        }
        staffUserRepository.save(wardUser);
        return "redirect:/admin/ward-members?created=1";
    }

    @GetMapping("/admin/ward-members/{id}")
    public String viewWardMember(@PathVariable("id") Long staffId, HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        StaffUser wardMember = staffUserRepository.findById(staffId).orElse(null);
        if (wardMember == null || !"WARD".equalsIgnoreCase(wardMember.getRole())) {
            return "redirect:/admin/ward-members?error=notfound";
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("wardMember", wardMember);
        return "admin-ward-member-profile";
    }

    @PostMapping("/admin/ward-members/{id}/update")
    public String updateWardMember(@PathVariable("id") Long staffId,
                                   @RequestParam(value = "fullName", required = false) String fullName,
                                   @RequestParam(value = "phone", required = false) String phone,
                                   @RequestParam(value = "password", required = false) String password,
                                   @RequestParam(value = "wardNo", required = false) Integer wardNo,
                                   @RequestParam(value = "wardZone", required = false) String wardZone,
                                   HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        StaffUser wardMember = staffUserRepository.findById(staffId).orElse(null);
        if (wardMember == null || !"WARD".equalsIgnoreCase(wardMember.getRole())) {
            return "redirect:/admin/ward-members?error=notfound";
        }

        if (fullName != null) {
            wardMember.setFullName(fullName.isBlank() ? null : fullName.trim());
        }
        if (phone != null) {
            wardMember.setPhone(phone.isBlank() ? null : phone.trim());
        }
        if (password != null && !password.isBlank()) {
            wardMember.setPassword(password);
        }
        if (wardNo != null) {
            wardMember.setWardNo(wardNo);
        }
        if (wardZone != null && !wardZone.isBlank()) {
            wardMember.setWardZone(wardZone.trim().toUpperCase());
        }

        var ward = wardService.resolveWard(wardMember.getWardNo(), wardMember.getWardZone());
        if (ward != null) {
            wardMember.setWardId(ward.getId());
        }

        staffUserRepository.save(wardMember);
        return "redirect:/admin/ward-members/" + staffId + "?saved=1";
    }

    @PostMapping("/admin/ward-members/{id}/delete")
    public String deleteWardMember(@PathVariable("id") Long staffId, HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        StaffUser wardMember = staffUserRepository.findById(staffId).orElse(null);
        if (wardMember == null || !"WARD".equalsIgnoreCase(wardMember.getRole())) {
            return "redirect:/admin/ward-members?error=notfound";
        }

        staffUserRepository.deleteById(staffId);
        return "redirect:/admin/ward-members?deleted=1";
    }

    @PostMapping("/admin/notices/citizen")
    public String publishCitizenNotice(@RequestParam("noticeMessage") String noticeMessage, HttpSession session) {
        return publishNotice("CITIZEN", noticeMessage, session);
    }

    @PostMapping("/admin/notices/ward")
    public String publishWardNotice(@RequestParam("noticeMessage") String noticeMessage, HttpSession session) {
        return publishNotice("WARD", noticeMessage, session);
    }

    @PostMapping("/admin/notices/{id}/delete")
    public String deleteNotice(@PathVariable("id") Long noticeId, HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        noticeRepository.findById(noticeId).ifPresent(notice -> {
            notice.setActive(false);
            noticeRepository.save(notice);
        });
        return "redirect:/admin/notices";
    }

    private String publishNotice(String targetType, String noticeMessage, HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        if (noticeMessage != null && !noticeMessage.isBlank()) {
            Notice notice = new Notice();
            notice.setMessage(noticeMessage.trim());
            notice.setTargetType(targetType);
            notice.setActive(true);
            notice.setCreatedAt(LocalDateTime.now());
            notice.setCreatedBy(staffUser.getUsername());
            noticeRepository.save(notice);
        }

        return "redirect:/admin/notices";
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
