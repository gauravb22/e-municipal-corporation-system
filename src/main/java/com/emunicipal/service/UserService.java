package com.emunicipal.service;

import com.emunicipal.entity.User;
import com.emunicipal.entity.Ward;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.util.PhoneNumberUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WardService wardService;

    public UserService(UserRepository userRepository, WardService wardService) {
        this.userRepository = userRepository;
        this.wardService = wardService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public User findByPhone(String phone) {
        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(phone);
        if (normalizedPhone == null) {
            return null;
        }
        return userRepository.findByPhone(normalizedPhone);
    }

    public boolean phoneInUseByOtherUser(String phone, Long userId) {
        String normalizedPhone = PhoneNumberUtil.normalizeIndianPhone(phone);
        if (normalizedPhone == null) {
            return false;
        }
        if (userId == null) {
            return userRepository.existsByPhone(normalizedPhone);
        }
        return userRepository.existsByPhoneAndIdNot(normalizedPhone, userId);
    }

    public User createUser(User user) {
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        if (user.getActive() == null) {
            user.setActive(true);
        }

        String normalizedZone = normalizeZone(user.getWardZone());
        user.setWardZone(normalizedZone);
        Ward ward = wardService.resolveWard(user.getWardNo(), normalizedZone);
        if (ward != null) {
            user.setWardId(ward.getId());
            user.setWardNo(ward.getWardNo());
            user.setWardZone(ward.getWardZone());
        } else {
            user.setWardId(null);
        }

        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long userId, User patch) {
        Optional<User> existingOpt = userRepository.findById(userId);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        User existing = existingOpt.get();

        if (patch.getFullName() != null) {
            existing.setFullName(patch.getFullName());
        }
        if (patch.getEmail() != null) {
            existing.setEmail(patch.getEmail());
        }
        if (patch.getPassword() != null) {
            existing.setPassword(patch.getPassword());
        }
        if (patch.getPhone() != null) {
            existing.setPhone(patch.getPhone());
        }
        if (patch.getAddress() != null) {
            existing.setAddress(patch.getAddress());
        }
        if (patch.getHouseNo() != null) {
            existing.setHouseNo(patch.getHouseNo());
        }
        if (patch.getPhotoBase64() != null) {
            existing.setPhotoBase64(patch.getPhotoBase64());
        }
        if (patch.getActive() != null) {
            existing.setActive(patch.getActive());
        }

        if (patch.getWardNo() != null || patch.getWardZone() != null) {
            Integer wardNo = patch.getWardNo() != null ? patch.getWardNo() : existing.getWardNo();
            String wardZone = patch.getWardZone() != null ? patch.getWardZone() : existing.getWardZone();
            String normalizedZone = normalizeZone(wardZone);

            existing.setWardNo(wardNo);
            existing.setWardZone(normalizedZone);

            Ward ward = wardService.resolveWard(wardNo, normalizedZone);
            if (ward != null) {
                existing.setWardId(ward.getId());
                existing.setWardNo(ward.getWardNo());
                existing.setWardZone(ward.getWardZone());
            } else {
                existing.setWardId(null);
            }
        }

        return Optional.of(userRepository.save(existing));
    }

    public boolean deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            return false;
        }
        userRepository.deleteById(userId);
        return true;
    }

    private String normalizeZone(String wardZone) {
        if (wardZone == null || wardZone.isBlank()) {
            return null;
        }
        return wardZone.trim().toUpperCase();
    }
}
