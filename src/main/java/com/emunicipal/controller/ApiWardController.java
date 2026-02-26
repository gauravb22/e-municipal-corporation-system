package com.emunicipal.controller;

import com.emunicipal.entity.Ward;
import com.emunicipal.repository.WardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wards")
public class ApiWardController {

    private final WardRepository wardRepository;

    public ApiWardController(WardRepository wardRepository) {
        this.wardRepository = wardRepository;
    }

    @GetMapping
    public ResponseEntity<List<WardResponse>> getWards() {
        List<WardResponse> wards = wardRepository.findAll().stream()
                .sorted(Comparator.comparing(Ward::getWardNo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Ward::getWardZone, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(wards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWardById(@PathVariable("id") Long wardId) {
        Ward ward = wardRepository.findById(wardId).orElse(null);
        if (ward == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ward not found."));
        }
        return ResponseEntity.ok(toResponse(ward));
    }

    @PostMapping
    public ResponseEntity<?> createWard(@RequestBody CreateWardRequest request) {
        if (request == null) {
            return badRequest("Request body is required.");
        }
        if (request.wardNo() == null) {
            return badRequest("wardNo is required.");
        }
        if (isBlank(request.wardZone())) {
            return badRequest("wardZone is required.");
        }

        String normalizedZone = request.wardZone().trim().toUpperCase();
        Ward existing = wardRepository.findByWardNoAndWardZone(request.wardNo(), normalizedZone);
        if (existing != null) {
            return badRequest("Ward already exists for wardNo " + request.wardNo() + " and wardZone " + normalizedZone + ".");
        }

        Ward ward = new Ward();
        ward.setWardNo(request.wardNo());
        ward.setWardZone(normalizedZone);
        ward.setName(isBlank(request.name()) ? null : request.name().trim());
        ward.setCreatedAt(LocalDateTime.now());

        Ward saved = wardRepository.save(ward);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private WardResponse toResponse(Ward ward) {
        return new WardResponse(
                ward.getId(),
                ward.getWardNo(),
                ward.getWardZone(),
                ward.getName(),
                ward.getCreatedAt()
        );
    }

    public record CreateWardRequest(Integer wardNo, String wardZone, String name) {
    }

    public record WardResponse(Long id, Integer wardNo, String wardZone, String name, LocalDateTime createdAt) {
    }
}
