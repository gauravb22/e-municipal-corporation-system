package com.emunicipal.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.emunicipal.entity.StaffUser;
import com.emunicipal.repository.StaffUserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class StaffAuthController {

    @Autowired
    private StaffUserRepository staffUserRepository;

    @GetMapping("/ward-login")
    public String wardLoginPage() {
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

        model.addAttribute("staffUser", staffUser);
        return "ward-dashboard";
    }
}
