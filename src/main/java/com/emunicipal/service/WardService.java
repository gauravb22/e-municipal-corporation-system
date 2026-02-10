package com.emunicipal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.emunicipal.entity.Ward;
import com.emunicipal.repository.WardRepository;

@Service
public class WardService {

    @Autowired
    private WardRepository wardRepository;

    public Ward resolveWard(Integer wardNo, String wardZone) {
        if (wardNo == null || wardZone == null || wardZone.isBlank()) {
            return null;
        }
        String normalizedZone = wardZone.trim().toUpperCase();
        Ward existing = wardRepository.findByWardNoAndWardZone(wardNo, normalizedZone);
        if (existing != null) {
            return existing;
        }
        Ward ward = new Ward();
        ward.setWardNo(wardNo);
        ward.setWardZone(normalizedZone);
        return wardRepository.save(ward);
    }
}

