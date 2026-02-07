package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import com.emunicipal.entity.User;
import com.emunicipal.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    /*
    =====================================
    OPEN PROFILE PAGE
    =====================================
    */

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        // if not logged in
        if (user == null) {
            return "redirect:/login";
        }

        // get fresh data from database
        User freshUser = userRepository.findById(user.getId()).orElse(user);

        session.setAttribute("user", freshUser);

        model.addAttribute("user", freshUser);

        return "profile"; // profile.html
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
            @RequestParam(value = "currentPassword", required = false) String currentPassword,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        // update basic details
        if (fullName != null && !fullName.isEmpty()) {
            user.setFullName(fullName);
        }

        user.setAddress(address);
        user.setHouseNo(houseNo);
        user.setWardNo(wardNo);
        user.setWardZone(wardZone);

        // password change
        if (newPassword != null && !newPassword.isEmpty()) {

            if (!user.getPassword().equals(currentPassword)) {

                model.addAttribute("error", "Current password incorrect");
                model.addAttribute("user", user);

                return "profile";
            }

            if (!newPassword.equals(confirmPassword)) {

                model.addAttribute("error", "Passwords do not match");
                model.addAttribute("user", user);

                return "profile";
            }

            user.setPassword(newPassword);
        }

        userRepository.save(user);

        session.setAttribute("user", user);

        model.addAttribute("success", "Profile updated successfully");
        model.addAttribute("user", user);

        return "profile";
    }

}
