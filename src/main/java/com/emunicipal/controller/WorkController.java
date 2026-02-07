package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.emunicipal.entity.WardWork;
import com.emunicipal.entity.User;
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

        if (user == null) {
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

        // If not logged in
        if (user == null) {
            return List.of();
        }

        Integer wardNo = user.getWardNo();

        // Safety check
        if (wardNo == null) {
            return List.of();
        }

        return repo.findByWardNo(wardNo);
    }

}
