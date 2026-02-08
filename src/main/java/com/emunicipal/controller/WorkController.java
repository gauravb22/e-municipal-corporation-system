package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.emunicipal.entity.WardWork;
import com.emunicipal.entity.User;
import com.emunicipal.entity.StaffUser;
import com.emunicipal.repository.WardWorkRepository;

import jakarta.servlet.http.HttpSession;

import java.util.List;

@Controller
public class WorkController {

    @Autowired
    private WardWorkRepository repo;


    /*
     ===============================
     LOAD WARD WORK PAGE
     ===============================
    */

    @GetMapping("/ward-works")
    public String wardWorksPage(HttpSession session) {

        // Check login session
        User user = (User) session.getAttribute("user");
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");

        if (user == null && staffUser == null) {
            return "redirect:/login";
        }

        return "ward-works";
    }



    /*
     ===============================
     API - GET USER WARD WORKS
     Automatically detect ward
     ===============================
    */

    @GetMapping("/api/ward-works")
    @ResponseBody
    public List<WardWork> getWorks(HttpSession session) {

        User user = (User) session.getAttribute("user");
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");

        // If not logged in
        if (user == null && staffUser == null) {
            return List.of();
        }

        Integer wardNo = user != null ? user.getWardNo() : staffUser.getWardNo();

        // Safety check
        if (wardNo == null) {
            return List.of();
        }

        return repo.findByWardNo(wardNo);
    }

}
