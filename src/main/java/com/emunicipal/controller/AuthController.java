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
import com.emunicipal.service.OtpService;
import com.emunicipal.service.OtpService.OtpCheckResult;
import com.emunicipal.service.SmsService;
import com.emunicipal.service.NotificationService;
import com.emunicipal.service.WardService;
import com.emunicipal.util.ImageFormatValidator;
import com.emunicipal.util.PhoneNumberUtil;
import com.emunicipal.entity.Ward;

import jakarta.servlet.http.HttpSession;

import java.util.Objects;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuthController {
    private static final String OTP_CONTEXT_LOGIN = "citizenLoginOtp";
    private static final String OTP_CONTEXT_PROFILE_PASSWORD = "profilePasswordOtp";
    private static final String SESSION_PENDING_LOGIN_USER_ID = "pendingLoginUserId";
    private static final String SESSION_PENDING_LOGIN_PHONE = "pendingLoginPhone";
    private static final int LOGIN_OTP_EXPIRY_MINUTES = 5;
    private static final int PASSWORD_OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_MAX_ATTEMPTS = 5;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private OtpService otpService;

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
        clearCitizenLoginOtp(session);
        return "login";
    }

    @PostMapping("/check-phone")
    @ResponseBody
    public Map<String, Object> checkPhone(@RequestBody Map<String, String> request,
                                          HttpSession session) {

        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(request.get("phone"));
        if (normalizedPhone == null) {
            return Map.of("exists", false, "message", "Invalid phone number");
        }

        session.setAttribute("phone", normalizedPhone);

        User user = userRepository.findByPhone(normalizedPhone);

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
        if (!model.containsAttribute("phone")) {
            model.addAttribute("phone", "");
        }
        return "citizen-password-login";
    }

    @PostMapping("/citizen-password-login")
    public String citizenPasswordLogin(@RequestParam("phone") String phone,
                                       @RequestParam("password") String password,
                                       HttpSession session,
                                       Model model) {
        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(phone);
        String enteredPhone = phone == null ? "" : phone.trim();
        String normalizedPassword = password == null ? "" : password;

        if (normalizedPhone == null || normalizedPassword.isBlank()) {
            model.addAttribute("error", "Enter valid 10-digit mobile number and password.");
            model.addAttribute("phone", enteredPhone);
            return "citizen-password-login";
        }

        User user = userRepository.findByPhone(normalizedPhone);
        if (user == null || !normalizedPassword.equals(user.getPassword())) {
            model.addAttribute("error", "Invalid mobile number or password.");
            model.addAttribute("phone", normalizedPhone);
            return "citizen-password-login";
        }
        if (user.getActive() != null && !user.getActive()) {
            model.addAttribute("error", "Account is blocked. Please contact administration.");
            model.addAttribute("phone", normalizedPhone);
            return "citizen-password-login";
        }

        session.removeAttribute("staffUser");
        session.removeAttribute("staffRole");
        clearCitizenLoginOtp(session);
        session.setAttribute("user", user);
        session.setAttribute("authenticated", true);

        return "redirect:/dashboard";
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

        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(phone);
        if (normalizedPhone == null) {
            return "redirect:/login";
        }

        session.removeAttribute("staffUser");
        session.removeAttribute("staffRole");

        User user = userRepository.findByPhone(normalizedPhone);

        if (user == null) {
            return "redirect:/login";
        }
        if (user.getActive() != null && !user.getActive()) {
            model.addAttribute("error", "Account is blocked. Please contact administration.");
            return "login";
        }

        String otp = otpService.issueOtp(
                session,
                OTP_CONTEXT_LOGIN,
                user.getId(),
                normalizedPhone,
                LOGIN_OTP_EXPIRY_MINUTES,
                OTP_MAX_ATTEMPTS
        );

        session.setAttribute("phone", normalizedPhone);
        session.setAttribute(SESSION_PENDING_LOGIN_USER_ID, user.getId());
        session.setAttribute(SESSION_PENDING_LOGIN_PHONE, normalizedPhone);
        session.removeAttribute("user");
        session.removeAttribute("authenticated");

        boolean sent = smsService.sendOtp(normalizedPhone, otp);
        if (!sent) {
            clearCitizenLoginOtp(session);
            model.addAttribute("error", "Unable to send OTP right now. Please try again.");
        }

        model.addAttribute("otpSent", sent);
        model.addAttribute("phone", normalizedPhone);
        if (sent) {
            model.addAttribute("otpPreview", otp);
        }

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

        Long pendingUserId = asLong(session.getAttribute(SESSION_PENDING_LOGIN_USER_ID));
        String pendingPhone = PhoneNumberUtil.normalizeIndianPhone((String) session.getAttribute(SESSION_PENDING_LOGIN_PHONE));

        OtpCheckResult otpResult = otpService.verifyOtp(
                session,
                OTP_CONTEXT_LOGIN,
                otp,
                pendingUserId,
                pendingPhone
        );
        if (!otpResult.isSuccess()) {
            model.addAttribute("error", mapCitizenLoginOtpError(otpResult));
            model.addAttribute("phone", pendingPhone);
            model.addAttribute("otpSent", otpResult.status() != OtpService.OtpStatus.NO_CHALLENGE);
            return "otp";
        }

        if (pendingUserId == null) {
            clearCitizenLoginOtp(session);
            return "redirect:/login";
        }

        User user = userRepository.findById(pendingUserId).orElse(null);
        if (user == null || (user.getActive() != null && !user.getActive())) {
            clearCitizenLoginOtp(session);
            return "redirect:/login";
        }

        session.setAttribute("user", user);
        session.setAttribute("authenticated", true);
        clearCitizenLoginOtp(session);

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

        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(phone);
        if (normalizedPhone != null) {
            model.addAttribute("phone", normalizedPhone);
            session.setAttribute("phone", normalizedPhone);
        }

        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user,
                               Model model,
                               HttpSession session) {

        Objects.requireNonNull(user, "User data is required");

        String phone = PhoneNumberUtil.normalizeIndianPhone((String) session.getAttribute("phone"));

        if (phone == null) {
            model.addAttribute("error", "Invalid phone number");
            return "register";
        }

        user.setPhone(phone);

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
        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(phone);
        if (normalizedPhone == null) {
            return Map.of("success", false, "message", "Registered mobile number is invalid.");
        }

        String otp = otpService.issueOtp(
                session,
                OTP_CONTEXT_PROFILE_PASSWORD,
                user.getId(),
                normalizedPhone,
                PASSWORD_OTP_EXPIRY_MINUTES,
                OTP_MAX_ATTEMPTS
        );

        boolean sent = smsService.sendOtp(normalizedPhone, otp);
        if (!sent) {
            otpService.clear(session, OTP_CONTEXT_PROFILE_PASSWORD);
            return Map.of("success", false, "message", "Unable to send OTP right now. Please try again.");
        }
        return Map.of(
                "success", true,
                "message", "OTP generated. Check popup for verification code.",
                "otp", otp
        );
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

        String normalizedUserPhone = PhoneNumberUtil.normalizeIndianPhone(user.getPhone());
        if (normalizedUserPhone == null) {
            return "Registered mobile number changed. Please request OTP again.";
        }

        OtpCheckResult otpResult = otpService.verifyOtp(
                session,
                OTP_CONTEXT_PROFILE_PASSWORD,
                enteredOtp,
                user.getId(),
                normalizedUserPhone
        );

        if (otpResult.isSuccess()) {
            return null;
        }

        return switch (otpResult.status()) {
            case MISSING -> "Please enter OTP sent on your mobile number.";
            case NO_CHALLENGE -> "Please request OTP first.";
            case ACCOUNT_MISMATCH -> "OTP is not valid for this account. Please request a new OTP.";
            case PHONE_MISMATCH -> "Registered mobile number changed. Please request OTP again.";
            case EXPIRED -> "OTP expired. Please request a new OTP.";
            case TOO_MANY_ATTEMPTS -> "Too many invalid attempts. Please request a new OTP.";
            case INVALID -> "Invalid OTP. Please enter correct OTP.";
            default -> "Unable to verify OTP. Please request a new OTP.";
        };
    }

    private String mapCitizenLoginOtpError(OtpCheckResult otpResult) {
        return switch (otpResult.status()) {
            case MISSING -> "Please enter the 6-digit OTP.";
            case NO_CHALLENGE -> "Please request OTP first.";
            case ACCOUNT_MISMATCH, PHONE_MISMATCH -> "OTP is not valid for this login. Please request a new OTP.";
            case EXPIRED -> "OTP expired. Please request a new OTP.";
            case TOO_MANY_ATTEMPTS -> "Too many invalid attempts. Please request a new OTP.";
            case INVALID -> "Invalid OTP. Please try again.";
            default -> "Unable to verify OTP. Please request a new OTP.";
        };
    }

    private void clearCitizenLoginOtp(HttpSession session) {
        otpService.clear(session, OTP_CONTEXT_LOGIN);
        session.removeAttribute(SESSION_PENDING_LOGIN_USER_ID);
        session.removeAttribute(SESSION_PENDING_LOGIN_PHONE);
        session.removeAttribute("phone");
    }

    private Long asLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private void clearProfilePasswordOtp(HttpSession session) {
        otpService.clear(session, OTP_CONTEXT_PROFILE_PASSWORD);
    }

}
