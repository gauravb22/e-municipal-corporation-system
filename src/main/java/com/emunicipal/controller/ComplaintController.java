package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.emunicipal.entity.User;
import com.emunicipal.entity.Complaint;
import com.emunicipal.service.ComplaintService;
import com.emunicipal.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private UserRepository userRepository;

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
            @RequestParam("rating") Integer rating,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        complaintService.submitFeedback(complaintId, solved, description, rating);
        
        System.out.println("================== FEEDBACK SUBMITTED ==================");
        System.out.println("Complaint ID: " + complaintId);
        System.out.println("Solved: " + solved);
        System.out.println("Rating: " + rating + " stars");
        System.out.println("Description: " + description);
        System.out.println("========================================================");
        
        return "redirect:/complaint-status";
    }
}

