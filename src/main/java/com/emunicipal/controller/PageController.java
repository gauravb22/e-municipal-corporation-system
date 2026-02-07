package com.emunicipal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // Works + Yojna page
    @GetMapping("/work-yojna")
    public String workPage() {
        return "work-yojna";   // opens work-yojna.html
    }

}
