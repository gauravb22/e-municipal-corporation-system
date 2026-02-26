package com.emunicipal.service;

import com.emunicipal.entity.Ward;
import com.emunicipal.repository.WardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WardService {

    private final WardRepository wardRepository;

    public WardService(WardRepository wardRepository) {
        this.wardRepository = wardRepository;
    }

    public List<Ward> getAllWards() {
        return wardRepository.findAll();
    }

    public Optional<Ward> getWardById(Long wardId) {
        return wardRepository.findById(wardId);
    }

    public Ward createWard(Ward ward) {
        ward.setWardZone(normalizeZone(ward.getWardZone()));
        if (ward.getCreatedAt() == null) {
            ward.setCreatedAt(LocalDateTime.now());
        }
        return wardRepository.save(ward);
    }

    public Optional<Ward> updateWard(Long wardId, Ward patch) {
        Optional<Ward> existingOpt = wardRepository.findById(wardId);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        Ward existing = existingOpt.get();
        if (patch.getWardNo() != null) {
            existing.setWardNo(patch.getWardNo());
        }
        if (patch.getWardZone() != null) {
            existing.setWardZone(normalizeZone(patch.getWardZone()));
        }
        if (patch.getName() != null) {
            existing.setName(patch.getName());
        }

        return Optional.of(wardRepository.save(existing));
    }

    public boolean deleteWard(Long wardId) {
        if (!wardRepository.existsById(wardId)) {
            return false;
        }
        wardRepository.deleteById(wardId);
        return true;
    }

    public Ward findByWardNoAndWardZone(Integer wardNo, String wardZone) {
        if (wardNo == null || wardZone == null || wardZone.isBlank()) {
            return null;
        }
        return wardRepository.findByWardNoAndWardZone(wardNo, wardZone.trim().toUpperCase());
    }

    public boolean wardExists(Integer wardNo, String wardZone) {
        if (wardNo == null || wardZone == null || wardZone.isBlank()) {
            return false;
        }
        return wardRepository.existsByWardNoAndWardZone(wardNo, wardZone.trim().toUpperCase());
    }

    public boolean wardExistsForOtherId(Integer wardNo, String wardZone, Long wardId) {
        if (wardNo == null || wardZone == null || wardZone.isBlank() || wardId == null) {
            return false;
        }
        return wardRepository.existsByWardNoAndWardZoneAndIdNot(wardNo, wardZone.trim().toUpperCase(), wardId);
    }

    public Ward resolveWard(Integer wardNo, String wardZone) {
        if (wardNo == null || wardZone == null || wardZone.isBlank()) {
            return null;
        }
        String normalizedZone = normalizeZone(wardZone);
        Ward existing = wardRepository.findByWardNoAndWardZone(wardNo, normalizedZone);
        if (existing != null) {
            return existing;
        }
        Ward ward = new Ward();
        ward.setWardNo(wardNo);
        ward.setWardZone(normalizedZone);
        return wardRepository.save(ward);
    }

    private String normalizeZone(String wardZone) {
        if (wardZone == null || wardZone.isBlank()) {
            return null;
        }
        return wardZone.trim().toUpperCase();
    }
}
