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
import com.emunicipal.entity.WardWork;
import com.emunicipal.entity.User;
import com.emunicipal.repository.StaffUserRepository;
import com.emunicipal.repository.ComplaintRepository;
import com.emunicipal.repository.NoticeRepository;
import com.emunicipal.repository.WardWorkRepository;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.service.ComplaintService;
import com.emunicipal.service.WardService;

import jakarta.servlet.http.HttpSession;
import java.time.Year;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

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
    @Autowired
    private WardWorkRepository wardWorkRepository;
    @Autowired
    private UserRepository userRepository;

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

    @GetMapping("/admin/users")
    public String adminUsers(HttpSession session,
                             Model model,
                             @RequestParam(value = "wardNo", required = false) Integer wardNo,
                             @RequestParam(value = "wardZone", required = false) String wardZone) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        String normalizedZone = (wardZone == null || wardZone.isBlank()) ? null : wardZone.trim().toUpperCase();
        List<User> allUsers = userRepository.findAll();
        allUsers.sort(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<User> users = new ArrayList<>();
        for (User user : allUsers) {
            boolean wardMatch = wardNo == null || (user.getWardNo() != null && user.getWardNo().equals(wardNo));
            boolean zoneMatch = normalizedZone == null || (user.getWardZone() != null && normalizedZone.equalsIgnoreCase(user.getWardZone()));
            if (wardMatch && zoneMatch) {
                users.add(user);
            }
        }

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long activeUsers = 0;
        long newUsers = 0;
        for (User user : allUsers) {
            boolean active = (user.getActive() == null || user.getActive());
            if (active) {
                activeUsers++;
            }
            if (user.getCreatedAt() != null && user.getCreatedAt().isAfter(since)) {
                newUsers++;
            }
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("users", users);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("newUsers", newUsers);
        model.addAttribute("selectedWardNo", wardNo);
        model.addAttribute("selectedWardZone", normalizedZone);
        return "admin-users";
    }

    @PostMapping("/admin/users/{id}/block")
    public String blockUser(@PathVariable("id") Long userId, HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        userRepository.findById(userId).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/unblock")
    public String unblockUser(@PathVariable("id") Long userId, HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        userRepository.findById(userId).ifPresent(user -> {
            user.setActive(true);
            userRepository.save(user);
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Long userId, HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        }
        return "redirect:/admin/users?deleted=1";
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

    @GetMapping("/admin/ward-reports")
    public String adminWardReports(HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("wardMembers", staffUserRepository.findByRoleIgnoreCaseOrderByWardNoAscWardZoneAscUsernameAsc("WARD"));
        return "admin-ward-reports";
    }

    @GetMapping("/admin/ward-reports/{id}")
    public String adminWardReport(@PathVariable("id") Long staffId, HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        StaffUser wardMember = staffUserRepository.findById(staffId).orElse(null);
        if (wardMember == null || !"WARD".equalsIgnoreCase(wardMember.getRole())) {
            return "redirect:/admin/ward-reports?error=notfound";
        }

        Integer wardNo = wardMember.getWardNo();
        if (wardNo == null) {
            return "redirect:/admin/ward-reports?error=ward";
        }

        List<String> completedStatuses = List.of("completed", "verified", "solved");
        List<String> pendingStatuses = List.of("submitted", "assigned", "approved", "in_progress", "pending", "overdue");

        long totalComplaints = complaintRepository.countByWardNo(wardNo);
        long completedComplaints = complaintRepository.countByWardNoAndStatusIn(wardNo, completedStatuses);
        long pendingComplaints = complaintRepository.countByWardNoAndStatusIn(wardNo, pendingStatuses);
        long overdueComplaints = complaintRepository.countByWardNoAndStatusIn(wardNo, List.of("overdue"));

        double completionRate = totalComplaints > 0 ? (completedComplaints * 100.0 / totalComplaints) : 0.0;
        double overdueRate = totalComplaints > 0 ? (overdueComplaints * 100.0 / totalComplaints) : 0.0;

        Double avgRating = complaintRepository.getAverageRatingByWard(wardNo);
        long totalPosts = wardWorkRepository.countByWardNo(wardNo);

        int currentYear = Year.now().getValue();
        int yearsToShow = 5;
        List<YearSummary> yearSummaries = new ArrayList<>();
        for (int y = currentYear; y >= currentYear - (yearsToShow - 1); y--) {
            LocalDateTime start = LocalDate.of(y, 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(y, 12, 31).atTime(23, 59, 59);

            long yearTotal = complaintRepository.countByWardNoAndCreatedAtBetween(wardNo, start, end);
            long yearCompleted = complaintRepository.countByWardNoAndStatusInAndCreatedAtBetween(wardNo, completedStatuses, start, end);
            long yearPending = complaintRepository.countByWardNoAndStatusInAndCreatedAtBetween(wardNo, pendingStatuses, start, end);
            long yearOverdue = complaintRepository.countByWardNoAndStatusAndCreatedAtBetween(wardNo, "overdue", start, end);

            double yearOverdueRate = yearTotal > 0 ? (yearOverdue * 100.0 / yearTotal) : 0.0;
            yearSummaries.add(new YearSummary(y, yearTotal, yearCompleted, yearPending, yearOverdue, yearOverdueRate));
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("wardMember", wardMember);
        model.addAttribute("totalComplaints", totalComplaints);
        model.addAttribute("completedComplaints", completedComplaints);
        model.addAttribute("pendingComplaints", pendingComplaints);
        model.addAttribute("overdueComplaints", overdueComplaints);
        model.addAttribute("completionRate", String.format("%.1f", completionRate));
        model.addAttribute("overdueRate", String.format("%.1f", overdueRate));
        model.addAttribute("avgRating", avgRating != null ? String.format("%.1f", avgRating) : "0.0");
        model.addAttribute("totalPosts", totalPosts);
        model.addAttribute("yearSummaries", yearSummaries);
        return "admin-ward-report";
    }

    @GetMapping("/admin/ward-works")
    public String adminWardWorks(HttpSession session,
                                 Model model,
                                 @RequestParam(value = "year", required = false) Integer year,
                                 @RequestParam(value = "month", required = false) Integer month) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        Integer normalizedYear = (year == null ? 0 : year);
        Integer normalizedMonth = (month == null ? 0 : month);

        List<com.emunicipal.entity.WardWork> works;
        if (normalizedYear > 0 && normalizedMonth > 0) {
            YearMonth yearMonth = YearMonth.of(normalizedYear, normalizedMonth);
            LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            works = wardWorkRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        } else if (normalizedYear > 0) {
            LocalDateTime start = LocalDate.of(normalizedYear, 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(normalizedYear, 12, 31).atTime(23, 59, 59);
            works = wardWorkRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        } else {
            works = wardWorkRepository.findAllByOrderByCreatedAtDesc();
        }

        int currentYear = Year.now().getValue();
        List<Integer> yearOptions = new ArrayList<>();
        yearOptions.add(0); // All
        for (int y = currentYear; y >= currentYear - 10; y--) {
            yearOptions.add(y);
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("works", works);
        model.addAttribute("selectedYear", normalizedYear);
        model.addAttribute("selectedMonth", normalizedMonth);
        model.addAttribute("yearOptions", yearOptions);
        return "admin-ward-works";
    }

    @GetMapping("/admin/ward-reports/{id}/posts")
    public String adminWardMemberPosts(@PathVariable("id") Long staffId,
                                       HttpSession session,
                                       Model model,
                                       @RequestParam(value = "year", required = false) Integer year,
                                       @RequestParam(value = "month", required = false) Integer month) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        StaffUser wardMember = staffUserRepository.findById(staffId).orElse(null);
        if (wardMember == null || !"WARD".equalsIgnoreCase(wardMember.getRole())) {
            return "redirect:/admin/ward-reports?error=notfound";
        }

        Integer wardNo = wardMember.getWardNo();
        if (wardNo == null) {
            return "redirect:/admin/ward-reports?error=ward";
        }

        Integer normalizedYear = (year == null ? 0 : year);
        Integer normalizedMonth = (month == null ? 0 : month);

        LocalDateTime start = null;
        LocalDateTime end = null;
        if (normalizedYear > 0 && normalizedMonth > 0) {
            YearMonth ym = YearMonth.of(normalizedYear, normalizedMonth);
            start = ym.atDay(1).atStartOfDay();
            end = ym.atEndOfMonth().atTime(23, 59, 59);
        } else if (normalizedYear > 0) {
            start = LocalDate.of(normalizedYear, 1, 1).atStartOfDay();
            end = LocalDate.of(normalizedYear, 12, 31).atTime(23, 59, 59);
         }

        List<WardWork> wardWorks;
        if (start != null && end != null) {
            wardWorks = wardWorkRepository.findByWardNoAndCreatedAtBetweenOrderByCreatedAtDesc(wardNo, start, end);
        } else {
            wardWorks = wardWorkRepository.findByWardNoOrderByCreatedAtDesc(wardNo);
        }

        List<String> completedStatuses = List.of("completed", "verified", "solved");
        List<com.emunicipal.entity.Complaint> completedComplaints;
        if (start != null && end != null) {
            completedComplaints = complaintRepository.findByWardNoAndStatusInAndCompletedBetween(wardNo, completedStatuses, start, end);
        } else {
            LocalDateTime farPast = LocalDate.of(2000, 1, 1).atStartOfDay();
            LocalDateTime farFuture = LocalDate.of(2100, 12, 31).atTime(23, 59, 59);
            completedComplaints = complaintRepository.findByWardNoAndStatusInAndCompletedBetween(wardNo, completedStatuses, farPast, farFuture);
        }

        List<PostItem> feed = new ArrayList<>();
        for (WardWork w : wardWorks) {
            feed.add(PostItem.fromWardWork(w));
        }
        for (com.emunicipal.entity.Complaint c : completedComplaints) {
            feed.add(PostItem.fromCompletedComplaint(c));
        }
        feed.sort(Comparator.comparing(PostItem::getDateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        int currentYear = Year.now().getValue();
        List<Integer> yearOptions = new ArrayList<>();
        yearOptions.add(0);
        for (int y = currentYear; y >= currentYear - 10; y--) {
            yearOptions.add(y);
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("wardMember", wardMember);
        model.addAttribute("feed", feed);
        model.addAttribute("selectedYear", normalizedYear);
        model.addAttribute("selectedMonth", normalizedMonth);
        model.addAttribute("yearOptions", yearOptions);
        return "admin-ward-member-posts";
    }

    public static class PostItem {
        private final String typeLabel;
        private final String title;
        private final String description;
        private final String imageSrc;
        private final String metaLeft;
        private final String metaRight;
        private final LocalDateTime dateTime;

        private PostItem(String typeLabel, String title, String description, String imageSrc, String metaLeft, String metaRight, LocalDateTime dateTime) {
            this.typeLabel = typeLabel;
            this.title = title;
            this.description = description;
            this.imageSrc = imageSrc;
            this.metaLeft = metaLeft;
            this.metaRight = metaRight;
            this.dateTime = dateTime;
        }

        public static PostItem fromWardWork(WardWork w) {
            String imageSrc = null;
            if (w.getImageBase64() != null && !w.getImageBase64().isBlank()) {
                String raw = w.getImageBase64().trim();
                imageSrc = raw.startsWith("data:") ? raw : ("data:image/jpeg;base64," + raw);
            }
            String title = w.getTitle() != null ? w.getTitle() : "Ward Work";
            String desc = w.getDescription() != null ? w.getDescription() : "";
            String metaLeft = "Ward " + (w.getWardNo() != null ? w.getWardNo() : "-") + " • Zone " + (w.getWardZone() != null ? w.getWardZone() : "-");
            String metaRight = "Rating: " + (w.getRating() != null ? String.format("%.1f", w.getRating()) : "0.0");
            return new PostItem("WORK POST", title, desc, imageSrc, metaLeft, metaRight, w.getCreatedAt());
        }

        public static PostItem fromCompletedComplaint(com.emunicipal.entity.Complaint c) {
            String imageSrc = null;
            if (c.getDonePhotoPath() != null && !c.getDonePhotoPath().isBlank()) {
                imageSrc = c.getDonePhotoPath();
            } else if (c.getDonePhotoBase64() != null && !c.getDonePhotoBase64().isBlank()) {
                String raw = c.getDonePhotoBase64().trim();
                imageSrc = raw.startsWith("data:") ? raw : ("data:image/jpeg;base64," + raw);
            }

            String title = (c.getComplaintType() != null ? c.getComplaintType() : "Complaint") + " (Completed)";
            String desc = "";
            if (c.getLocation() != null && !c.getLocation().isBlank()) {
                desc = "Location: " + c.getLocation();
            }
            if (c.getHouseNo() != null && !c.getHouseNo().isBlank()) {
                desc = desc.isBlank() ? ("House: " + c.getHouseNo()) : (desc + "\nHouse: " + c.getHouseNo());
            }
            if (c.getFeedbackRating() != null) {
                desc = desc.isBlank() ? ("Star Rating: " + c.getFeedbackRating() + "/5") : (desc + "\nStar Rating: " + c.getFeedbackRating() + "/5");
            }

            String metaLeft = "Ward " + (c.getWardNo() != null ? c.getWardNo() : "-") + " • Zone " + (c.getWardZone() != null ? c.getWardZone() : "-");
            String metaRight = "Status: " + (c.getStatus() != null ? c.getStatus() : "-");
            LocalDateTime dt = (c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt());
            return new PostItem("COMPLAINT COMPLETED", title, desc, imageSrc, metaLeft, metaRight, dt);
        }

        public String getTypeLabel() { return typeLabel; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getImageSrc() { return imageSrc; }
        public String getMetaLeft() { return metaLeft; }
        public String getMetaRight() { return metaRight; }
        public LocalDateTime getDateTime() { return dateTime; }
    }

    public static class YearSummary {
        private final int year;
        private final long totalComplaints;
        private final long completedComplaints;
        private final long pendingComplaints;
        private final long overdueComplaints;
        private final double overdueRate;

        public YearSummary(int year, long totalComplaints, long completedComplaints, long pendingComplaints, long overdueComplaints, double overdueRate) {
            this.year = year;
            this.totalComplaints = totalComplaints;
            this.completedComplaints = completedComplaints;
            this.pendingComplaints = pendingComplaints;
            this.overdueComplaints = overdueComplaints;
            this.overdueRate = overdueRate;
        }

        public int getYear() { return year; }
        public long getTotalComplaints() { return totalComplaints; }
        public long getCompletedComplaints() { return completedComplaints; }
        public long getPendingComplaints() { return pendingComplaints; }
        public long getOverdueComplaints() { return overdueComplaints; }
        public double getOverdueRate() { return overdueRate; }
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
    public String updateWardProfile(@RequestParam(value = "fullName", required = false) String fullName,
                                    @RequestParam(value = "phone", required = false) String phone,
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

        if (fullName != null && !fullName.isBlank()) {
            staffUser.setFullName(fullName.trim());
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
