package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import com.emunicipal.entity.User;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.service.SmsService;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import java.util.Random;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmsService smsService;

    @GetMapping("/")
    public String home() {
        return "login";
    }

    @GetMapping("/login")
    public String loginPage() {
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

    @GetMapping("/otp-page")
    public String otpPage(@RequestParam("phone") String phone, HttpSession session, Model model) {
        // Validate phone
        if (phone == null || phone.length() != 10 || !phone.matches("[0-9]{10}")) {
            return "redirect:/login";
        }

        // Check if user exists
        User user = userRepository.findByPhone(phone);
        if (user == null) {
            return "redirect:/login";
        }

        // Generate OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        // Store in session
        session.setAttribute("phone", phone);
        session.setAttribute("otp", otp);
        session.setAttribute("user", user);

        // Send OTP via SMS (currently just logs to console)
        smsService.sendOtp(phone, otp);

        // Pass OTP to template for display
        model.addAttribute("phone", phone);
        model.addAttribute("otp", otp);
        return "otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String otp, HttpSession session, Model model) {
        String sessionOtp = (String) session.getAttribute("otp");

        if (sessionOtp == null || !sessionOtp.equals(otp)) {
            model.addAttribute("error", "Invalid OTP. Please try again.");
            model.addAttribute("phone", session.getAttribute("phone"));
            return "otp";
        }

        // OTP verified successfully
        session.setAttribute("authenticated", true);
        session.removeAttribute("otp");

        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(value = "phone", required = false) String phone, Model model) {
        if (phone != null && !phone.isEmpty()) {
            model.addAttribute("phone", phone);
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, Model model, HttpSession session) {
        Objects.requireNonNull(user, "User data is required");

        // Get phone from session (passed from login page)
        String phone = (String) session.getAttribute("phone");
        if (phone == null || phone.length() != 10) {
            model.addAttribute("error", "Invalid phone number");
            return "register";
        }

        // Set phone to user
        user.setPhone(phone);
        
        // Auto-generate email from phone if not provided
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            user.setEmail("user" + phone + "@municipal.local");
        }

        // Save user
        User saved = userRepository.save(user);

        // Set user in session and redirect to language page
        session.setAttribute("user", saved);

        return "redirect:/language";
    }

    @GetMapping("/language")
    public String languagePage() {
        return "language";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Object userObj = session.getAttribute("user");
        if (userObj == null) {
            return "redirect:/login";
        }
        
        User user = (User) userObj;
        // Set authenticated flag
        session.setAttribute("authenticated", true);
        model.addAttribute("user", user);
        
        return "dashboard";
    }

    @GetMapping("/my-profile")
    public String myProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Fetch fresh user from database to get latest data
        User freshUser = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", freshUser);
        
        model.addAttribute("user", freshUser);
        return "my-profile";
    }

    @GetMapping("/profile")
    public String profileAlias() {
        return "redirect:/my-profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "houseNo", required = false) String houseNo,
            @RequestParam(value = "wardNo", required = false) Integer wardNo,
            @RequestParam(value = "wardZone", required = false) String wardZone,
            @RequestParam(value = "currentPassword", required = false) String currentPassword,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Update simple profile fields
        if (fullName != null && !fullName.isEmpty()) user.setFullName(fullName);
        user.setAddress(address);
        user.setHouseNo(houseNo);
        user.setWardNo(wardNo);
        user.setWardZone(wardZone);

        // Handle password change if requested
        if (newPassword != null && !newPassword.isEmpty()) {
            // Verify current password matches
            if (currentPassword == null || !currentPassword.equals(user.getPassword())) {
                model.addAttribute("error", "Current password is incorrect");
                model.addAttribute("user", user);
                return "my-profile";
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "New password and confirmation do not match");
                model.addAttribute("user", user);
                return "my-profile";
            }

            user.setPassword(newPassword);
        }

        // Save updated user
        userRepository.save(user);
        // Update session
        session.setAttribute("user", user);

        model.addAttribute("success", "Profile updated successfully");
        model.addAttribute("user", user);
        return "my-profile";
    }
}
