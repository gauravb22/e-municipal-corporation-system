package com.emunicipal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.emunicipal.entity.User;
import com.emunicipal.repository.UserRepository;
import java.util.Objects;

@Controller
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user) {
        Objects.requireNonNull(user, "user must not be null");
        userRepository.save(user);
        return "login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
