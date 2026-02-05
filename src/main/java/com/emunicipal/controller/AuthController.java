package com.emunicipal.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.gaurav.emunicipal.entity.User;
import com.gaurav.emunicipal.repository.UserRepository;

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
    public String registerUser(User user) {
        userRepository.save(user);
        return "login";
    }
}
