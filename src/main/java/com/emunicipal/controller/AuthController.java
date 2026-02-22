package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import com.emunicipal.entity.User;
import com.emunicipal.entity.Complaint;
import com.emunicipal.entity.Notice;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.repository.ComplaintRepository;
import com.emunicipal.repository.NoticeRepository;
import com.emunicipal.service.SmsService;
import com.emunicipal.service.NotificationService;
import com.emunicipal.service.WardService;
import com.emunicipal.util.ImageFormatValidator;
import com.emunicipal.entity.Ward;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuthController {
    private static final String SESSION_PROFILE_PASSWORD_OTP = "profilePasswordOtp";
    private static final String SESSION_PROFILE_PASSWORD_OTP_USER_ID = "profilePasswordOtpUserId";
    private static final String SESSION_PROFILE_PASSWORD_OTP_PHONE = "profilePasswordOtpPhone";
    private static final String SESSION_PROFILE_PASSWORD_OTP_EXPIRES_AT = "profilePasswordOtpExpiresAt";
    private static final int PASSWORD_OTP_EXPIRY_MINUTES = 5;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private WardService wardService;

    @Autowired
    private NotificationService notificationService;

    /*
    =====================================
    LOGIN
    =====================================
    */

    @GetMapping("/")
    public String home() {
        return "login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        session.removeAttribute("staffUser");
        session.removeAttribute("staffRole");
        return "login";
    }

    @PostMapping("/check-phone")
    @ResponseBody
    public Map<String, Object> checkPhone(@RequestBody Map<String, String> request) {

        String phone = request.get("phone");

        User user = userRepository.findByPhone(phone);

        if (user != null) {
            return Map.of("exists", true, "message", "Phone found");
        } else {
            return Map.of("exists", false, "message", "Phone not registered");
        }
    }

    @GetMapping("/citizen-password-login")
    public String citizenPasswordLoginPage(HttpSession session, Model model) {
        session.removeAttribute("staffUser");
        session.removeAttribute("staffRole");
        if (!model.containsAttribute("identifier")) {
            model.addAttribute("identifier", "");
        }
        return "citizen-password-login";
    }

    @PostMapping("/citizen-password-login")
    public String citizenPasswordLoginForm(@RequestParam("identifier") String identifier,
                                           @RequestParam("password") String password,
                                           HttpSession session,
                                           Model model) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim();
        String normalizedPassword = password == null ? "" : password;

        if (normalizedIdentifier.isBlank() || normalizedPassword.isBlank()) {
            model.addAttribute("error", "Phone/username and password are required.");
            model.addAttribute("identifier", normalizedIdentifier);
            return "citizen-password-login";
        }

        User user = findCitizenByIdentifier(normalizedIdentifier);
        if (user == null || !normalizedPassword.equals(user.getPassword())) {
            model.addAttribute("error", "Invalid phone/username or password.");
            model.addAttribute("identifier", normalizedIdentifier);
            return "citizen-password-login";
        }
        if (user.getActive() != null && !user.getActive()) {
            model.addAttribute("error", "Account is blocked. Please contact administration.");
            model.addAttribute("identifier", normalizedIdentifier);
            return "citizen-password-login";
        }

        session.removeAttribute("staffUser");
        session.removeAttribute("staffRole");
        session.removeAttribute("otp");
        session.removeAttribute("phone");
        session.setAttribute("user", user);
        session.setAttribute("authenticated", true);

        return "redirect:/dashboard";
    }

    @PostMapping("/citizen-login-password")
    @ResponseBody
    public Map<String, Object> citizenLoginPassword(@RequestBody Map<String, String> request, HttpSession session) {
        String identifier = request.get("identifier");
        String password = request.get("password");

        if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) {
            return Map.of("success", false, "message", "Phone/username and password are required.");
        }

        String normalizedIdentifier = identifier.trim();
        User user = findCitizenByIdentifier(normalizedIdentifier);

        if (user == null || !password.equals(user.getPassword())) {
            return Map.of("success", false, "message", "Invalid phone/username or password.");
        }
        if (user.getActive() != null && !user.getActive()) {
            return Map.of("success", false, "message", "Account is blocked. Please contact administration.");
        }

        session.removeAttribute("staffUser");
        session.removeAttribute("staffRole");
        session.removeAttribute("otp");
        session.removeAttribute("phone");
        session.setAttribute("user", user);
        session.setAttribute("authenticated", true);

        return Map.of("success", true, "redirect", "/dashboard");
    }

    /*
    =====================================
    OTP PAGE
    =====================================
    */

    @GetMapping("/otp-page")
    public String otpPage(@RequestParam("phone") String phone,
                          HttpSession session,
                          Model model) {

        if (phone == null || phone.length() != 10 || !phone.matches("[0-9]{10}")) {
            return "redirect:/login";
        }

        session.removeAttribute("staffUser");
        session.removeAttribute("staffRole");

        User user = userRepository.findByPhone(phone);

        if (user == null) {
            return "redirect:/login";
        }
        if (user.getActive() != null && !user.getActive()) {
            model.addAttribute("error", "Account is blocked. Please contact administration.");
            return "login";
        }

        String otp = String.format("%06d", new Random().nextInt(999999));

        session.setAttribute("phone", phone);
        session.setAttribute("otp", otp);
        session.setAttribute("user", user);

        smsService.sendOtp(phone, otp);

        model.addAttribute("phone", phone);
        model.addAttribute("otp", otp);

        return "otp";
    }

    /*
    =====================================
    VERIFY OTP
    =====================================
    */

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String otp,
                            HttpSession session,
                            Model model) {

        String sessionOtp = (String) session.getAttribute("otp");

        if (sessionOtp == null || !sessionOtp.equals(otp)) {

            model.addAttribute("error", "Invalid OTP. Please try again.");
            model.addAttribute("phone", session.getAttribute("phone"));

            return "otp";
        }

        session.setAttribute("authenticated", true);
        session.removeAttribute("otp");

        return "redirect:/dashboard";
    }

    /*
    =====================================
    REGISTER
    =====================================
    */

    @GetMapping("/register")
    public String registerPage(@RequestParam(value = "phone", required = false) String phone,
                               HttpSession session,
                               Model model) {

        String normalizedPhone = phone == null ? null : phone.trim();
        if (normalizedPhone != null && !normalizedPhone.isEmpty()) {
            if (normalizedPhone.matches("\\d{10}")) {
                session.setAttribute("phone", normalizedPhone);
                model.addAttribute("phone", normalizedPhone);
            } else {
                model.addAttribute("error", "Invalid phone number");
            }
        } else {
            String sessionPhone = (String) session.getAttribute("phone");
            if (sessionPhone != null && !sessionPhone.isBlank()) {
                model.addAttribute("phone", sessionPhone);
            }
        }

        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam(value = "phone", required = false) String phoneFromForm,
                               Model model,
                               HttpSession session) {

        Objects.requireNonNull(user, "User data is required");

        String phone = (String) session.getAttribute("phone");
        if ((phone == null || phone.isBlank()) && phoneFromForm != null && !phoneFromForm.isBlank()) {
            phone = phoneFromForm.trim();
            if (phone.matches("\\d{10}")) {
                session.setAttribute("phone", phone);
            }
        }

        if (phone == null || !phone.matches("\\d{10}")) {
            model.addAttribute("error", "Invalid phone number");
            model.addAttribute("phone", phoneFromForm);
            return "register";
        }

        user.setPhone(phone);

        String username = user.getUsername() != null ? user.getUsername().trim() : null;
        if (username == null || username.isBlank()) {
            model.addAttribute("error", "Username is required");
            return "register";
        }
        if (!username.matches("[A-Za-z0-9._-]{4,50}")) {
            model.addAttribute("error", "Username must be 4-50 characters and can contain letters, numbers, dot, underscore, and hyphen only");
            return "register";
        }
        User existingByUsername = userRepository.findByUsernameIgnoreCase(username);
        if (existingByUsername != null) {
            model.addAttribute("error", "Username already taken");
            return "register";
        }
        user.setUsername(username);

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            user.setEmail("user" + phone + "@municipal.local");
        }

        if (user.getCreatedAt() == null) {
            user.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (user.getActive() == null) {
            user.setActive(true);
        }

        Ward ward = wardService.resolveWard(user.getWardNo(), user.getWardZone());
        if (ward != null) {
            user.setWardId(ward.getId());
            user.setWardNo(ward.getWardNo());
            user.setWardZone(ward.getWardZone());
        }

        User saved = userRepository.save(user);

        session.setAttribute("user", saved);

        return "redirect:/language";
    }

    /*
    =====================================
    LANGUAGE PAGE
    =====================================
    */

    @GetMapping("/language")
    public String languagePage() {
        return "language";
    }

    /*
    =====================================
    DASHBOARD
    =====================================
    */

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session,
                            @RequestParam(value = "complaintSubmitted", required = false) String complaintSubmitted,
                            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        session.setAttribute("authenticated", true);

        model.addAttribute("user", user);
        model.addAttribute("complaintSubmitted", complaintSubmitted != null);

        List<Complaint> pendingFeedback = complaintRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(c -> "completed".equalsIgnoreCase(c.getStatus()) || "solved".equalsIgnoreCase(c.getStatus()))
                .filter(c -> c.getFeedbackSubmitted() == null || !c.getFeedbackSubmitted())
                .collect(Collectors.toList());
        model.addAttribute("pendingFeedback", pendingFeedback);

        List<Notice> citizenNotices = noticeRepository.findTop5ByTargetTypeAndActiveTrueOrderByCreatedAtDesc("CITIZEN");
        model.addAttribute("citizenNotices", citizenNotices);
        model.addAttribute("recentNotifications", notificationService.getRecentForUser(user.getId(), 10));
        model.addAttribute("unreadNotificationCount", notificationService.countUnread(user.getId()));

        return "dashboard";
    }

    /*
    =====================================
    PROFILE PAGE (profile.html)
    =====================================
    */

    @GetMapping({"/profile", "/my-profile"})
    public String profilePage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        User freshUser = userRepository.findById(user.getId()).orElse(user);

        session.setAttribute("user", freshUser);

        model.addAttribute("user", freshUser);

        return "my-profile";
    }

    @PostMapping("/profile/send-password-otp")
    @ResponseBody
    public Map<String, Object> sendProfilePasswordOtp(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Map.of("success", false, "message", "Session expired. Please login again.");
        }

        String phone = user.getPhone();
        if (phone == null || !phone.matches("\\d{10}")) {
            return Map.of("success", false, "message", "Registered mobile number is invalid.");
        }

        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        session.setAttribute(SESSION_PROFILE_PASSWORD_OTP, otp);
        session.setAttribute(SESSION_PROFILE_PASSWORD_OTP_USER_ID, user.getId());
        session.setAttribute(SESSION_PROFILE_PASSWORD_OTP_PHONE, phone);
        session.setAttribute(SESSION_PROFILE_PASSWORD_OTP_EXPIRES_AT, LocalDateTime.now().plusMinutes(PASSWORD_OTP_EXPIRY_MINUTES));

        smsService.sendOtp(phone, otp);
        return Map.of("success", true, "message", "OTP sent to your registered mobile number.");
    }

    /*
    =====================================
    UPDATE PROFILE
    =====================================
    */

    @PostMapping("/update-profile")
    public String updateProfile(

            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "houseNo", required = false) String houseNo,
            @RequestParam(value = "wardNo", required = false) Integer wardNo,
            @RequestParam(value = "wardZone", required = false) String wardZone,
            @RequestParam(value = "photoBase64", required = false) String photoBase64,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            @RequestParam(value = "passwordOtp", required = false) String passwordOtp,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        if (fullName != null && !fullName.isEmpty()) {
            user.setFullName(fullName);
        }

        user.setAddress(address);
        user.setHouseNo(houseNo);
        user.setWardNo(wardNo);
        user.setWardZone(wardZone);

        Ward ward = wardService.resolveWard(user.getWardNo(), user.getWardZone());
        if (ward != null) {
            user.setWardId(ward.getId());
            user.setWardNo(ward.getWardNo());
            user.setWardZone(ward.getWardZone());
        } else {
            user.setWardId(null);
        }

        if (photoBase64 != null && !photoBase64.isBlank()) {
            if (!ImageFormatValidator.isJpgDataUrl(photoBase64)) {
                model.addAttribute("error", "Only JPG format is allowed for profile photo");
                model.addAttribute("user", user);
                return "my-profile";
            }
            user.setPhotoBase64(photoBase64);
        }

        if (newPassword != null && !newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {

                model.addAttribute("error", "Passwords do not match");
                model.addAttribute("user", user);

                return "my-profile";
            }

            String otpValidationError = validateProfilePasswordOtp(session, user, passwordOtp);
            if (otpValidationError != null) {
                model.addAttribute("error", otpValidationError);
                model.addAttribute("user", user);
                return "my-profile";
            }

            user.setPassword(newPassword);
            clearProfilePasswordOtp(session);
        }

        userRepository.save(user);

        session.setAttribute("user", user);

        model.addAttribute("success", "Profile updated successfully");
        model.addAttribute("user", user);

        return "my-profile";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String validateProfilePasswordOtp(HttpSession session, User user, String enteredOtp) {
        if (enteredOtp == null || enteredOtp.isBlank()) {
            return "Please enter OTP sent on your mobile number.";
        }

        String sessionOtp = (String) session.getAttribute(SESSION_PROFILE_PASSWORD_OTP);
        Long sessionUserId = (Long) session.getAttribute(SESSION_PROFILE_PASSWORD_OTP_USER_ID);
        String sessionPhone = (String) session.getAttribute(SESSION_PROFILE_PASSWORD_OTP_PHONE);
        LocalDateTime expiresAt = (LocalDateTime) session.getAttribute(SESSION_PROFILE_PASSWORD_OTP_EXPIRES_AT);

        if (sessionOtp == null || sessionUserId == null || sessionPhone == null || expiresAt == null) {
            return "Please request OTP first.";
        }

        if (!Objects.equals(sessionUserId, user.getId())) {
            clearProfilePasswordOtp(session);
            return "OTP is not valid for this account. Please request a new OTP.";
        }

        if (user.getPhone() == null || !sessionPhone.equals(user.getPhone())) {
            clearProfilePasswordOtp(session);
            return "Registered mobile number changed. Please request OTP again.";
        }

        if (LocalDateTime.now().isAfter(expiresAt)) {
            clearProfilePasswordOtp(session);
            return "OTP expired. Please request a new OTP.";
        }

        if (!sessionOtp.equals(enteredOtp.trim())) {
            return "Invalid OTP. Please enter correct OTP.";
        }

        return null;
    }

    private void clearProfilePasswordOtp(HttpSession session) {
        session.removeAttribute(SESSION_PROFILE_PASSWORD_OTP);
        session.removeAttribute(SESSION_PROFILE_PASSWORD_OTP_USER_ID);
        session.removeAttribute(SESSION_PROFILE_PASSWORD_OTP_PHONE);
        session.removeAttribute(SESSION_PROFILE_PASSWORD_OTP_EXPIRES_AT);
    }

    private User findCitizenByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        String normalizedIdentifier = identifier.trim();
        User user = null;
        if (normalizedIdentifier.matches("\\d{10}")) {
            user = userRepository.findByPhone(normalizedIdentifier);
        }
        if (user == null) {
            user = userRepository.findByUsernameIgnoreCase(normalizedIdentifier);
        }
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(normalizedIdentifier);
        }
        return user;
    }

}
