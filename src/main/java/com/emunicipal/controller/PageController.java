package com.emunicipal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // Works + Yojna page
    @GetMapping("/works-yojna")
    public String workPage() {
        return "works-yojna";   // opens works-yojna.html
    }

}
