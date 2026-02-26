package com.emunicipal.controller;

import com.emunicipal.entity.User;
import com.emunicipal.entity.Ward;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.service.WardService;
import com.emunicipal.util.PhoneNumberUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/users")
public class ApiUserController {

    private final UserRepository userRepository;
    private final WardService wardService;

    public ApiUserController(UserRepository userRepository, WardService wardService) {
        this.userRepository = userRepository;
        this.wardService = wardService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(value = "wardNo", required = false) Integer wardNo,
            @RequestParam(value = "wardZone", required = false) String wardZone,
            @RequestParam(value = "active", required = false) Boolean active) {

        Stream<User> stream = userRepository.findAllByOrderByCreatedAtDesc().stream();
        if (wardNo != null) {
            stream = stream.filter(user -> wardNo.equals(user.getWardNo()));
        }
        if (!isBlank(wardZone)) {
            String normalizedZone = wardZone.trim().toUpperCase();
            stream = stream.filter(user -> user.getWardZone() != null && normalizedZone.equalsIgnoreCase(user.getWardZone()));
        }
        if (active != null) {
            if (active) {
                stream = stream.filter(user -> user.getActive() == null || user.getActive());
            } else {
                stream = stream.filter(user -> user.getActive() != null && !user.getActive());
            }
        }

        List<UserResponse> users = stream.map(this::toResponse).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found."));
        }
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        if (request == null) {
            return badRequest("Request body is required.");
        }
        if (isBlank(request.fullName())) {
            return badRequest("fullName is required.");
        }
        if (isBlank(request.password())) {
            return badRequest("password is required.");
        }

        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(request.phone());
        if (normalizedPhone == null) {
            return badRequest("Valid 10-digit phone number is required.");
        }

        User existing = userRepository.findByPhone(normalizedPhone);
        if (existing != null) {
            return badRequest("Phone is already registered.");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setPhone(normalizedPhone);
        user.setPassword(request.password());
        user.setAddress(blankToNull(request.address()));
        user.setHouseNo(blankToNull(request.houseNo()));
        user.setPhotoBase64(blankToNull(request.photoBase64()));
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(request.active() == null ? Boolean.TRUE : request.active());

        if (isBlank(request.email())) {
            user.setEmail("user" + normalizedPhone + "@municipal.local");
        } else {
            user.setEmail(request.email().trim());
        }

        Integer wardNo = request.wardNo();
        String wardZone = blankToNull(request.wardZone());
        if (wardZone != null) {
            wardZone = wardZone.toUpperCase();
        }
        user.setWardNo(wardNo);
        user.setWardZone(wardZone);

        Ward ward = wardService.resolveWard(wardNo, wardZone);
        if (ward != null) {
            user.setWardId(ward.getId());
            user.setWardNo(ward.getWardNo());
            user.setWardZone(ward.getWardZone());
        }

        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getHouseNo(),
                user.getWardNo(),
                user.getWardZone(),
                user.getWardId(),
                user.getPhotoBase64(),
                user.getActive(),
                user.getCreatedAt()
        );
    }

    public record CreateUserRequest(
            String fullName,
            String email,
            String password,
            String phone,
            String address,
            String houseNo,
            Integer wardNo,
            String wardZone,
            String photoBase64,
            Boolean active
    ) {
    }

    public record UserResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            String address,
            String houseNo,
            Integer wardNo,
            String wardZone,
            Long wardId,
            String photoBase64,
            Boolean active,
            LocalDateTime createdAt
    ) {
    }
}
