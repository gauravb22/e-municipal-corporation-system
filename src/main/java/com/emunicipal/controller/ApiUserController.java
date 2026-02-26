package com.emunicipal.controller;

import com.emunicipal.entity.User;
import com.emunicipal.service.UserService;
import com.emunicipal.util.PhoneNumberUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    private final UserService userService;

    public ApiUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(value = "wardNo", required = false) Integer wardNo,
            @RequestParam(value = "wardZone", required = false) String wardZone,
            @RequestParam(value = "active", required = false) Boolean active) {

        Stream<User> stream = userService.getAllUsers().stream();
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
        User user = userService.getUserById(userId).orElse(null);
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

        if (userService.phoneInUseByOtherUser(normalizedPhone, null)) {
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

        user.setWardNo(request.wardNo());
        user.setWardZone(blankToNull(request.wardZone()));

        User saved = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long userId,
                                        @RequestBody UpdateUserRequest request) {
        if (request == null) {
            return badRequest("Request body is required.");
        }

        User existing = userService.getUserById(userId).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found."));
        }

        User patch = new User();

        if (!isBlank(request.fullName())) {
            patch.setFullName(request.fullName().trim());
        }
        if (!isBlank(request.email())) {
            patch.setEmail(request.email().trim());
        }
        if (!isBlank(request.password())) {
            patch.setPassword(request.password());
        }

        if (request.phone() != null) {
            String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(request.phone());
            if (normalizedPhone == null) {
                return badRequest("Valid 10-digit phone number is required.");
            }
            if (userService.phoneInUseByOtherUser(normalizedPhone, userId)) {
                return badRequest("Phone is already registered.");
            }
            patch.setPhone(normalizedPhone);
        }

        if (request.address() != null) {
            patch.setAddress(blankToNull(request.address()));
        }
        if (request.houseNo() != null) {
            patch.setHouseNo(blankToNull(request.houseNo()));
        }
        if (request.wardNo() != null) {
            patch.setWardNo(request.wardNo());
        }
        if (request.wardZone() != null) {
            patch.setWardZone(blankToNull(request.wardZone()));
        }
        if (request.photoBase64() != null) {
            patch.setPhotoBase64(blankToNull(request.photoBase64()));
        }
        if (request.active() != null) {
            patch.setActive(request.active());
        }

        User saved = userService.updateUser(userId, patch).orElse(null);
        if (saved == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found."));
        }
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long userId) {
        boolean deleted = userService.deleteUser(userId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found."));
        }
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
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

    public record UpdateUserRequest(
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
