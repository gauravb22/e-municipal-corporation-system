package com.emunicipal.controller;

import com.emunicipal.entity.Ward;
import com.emunicipal.service.WardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    private final WardService wardService;

    public ApiWardController(WardService wardService) {
        this.wardService = wardService;
    }

    @GetMapping
    public ResponseEntity<List<WardResponse>> getWards() {
        List<WardResponse> wards = wardService.getAllWards().stream()
                .sorted(Comparator.comparing(Ward::getWardNo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Ward::getWardZone, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(wards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWardById(@PathVariable("id") Long wardId) {
        Ward ward = wardService.getWardById(wardId).orElse(null);
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
        if (wardService.wardExists(request.wardNo(), normalizedZone)) {
            return badRequest("Ward already exists for wardNo " + request.wardNo() + " and wardZone " + normalizedZone + ".");
        }

        Ward ward = new Ward();
        ward.setWardNo(request.wardNo());
        ward.setWardZone(normalizedZone);
        ward.setName(isBlank(request.name()) ? null : request.name().trim());
        ward.setCreatedAt(LocalDateTime.now());

        Ward saved = wardService.createWard(ward);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWard(@PathVariable("id") Long wardId,
                                        @RequestBody UpdateWardRequest request) {
        if (request == null) {
            return badRequest("Request body is required.");
        }

        Ward existing = wardService.getWardById(wardId).orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ward not found."));
        }

        Integer targetWardNo = request.wardNo() != null ? request.wardNo() : existing.getWardNo();
        String targetWardZone = !isBlank(request.wardZone()) ? request.wardZone().trim().toUpperCase() : existing.getWardZone();
        if (targetWardNo == null || isBlank(targetWardZone)) {
            return badRequest("wardNo and wardZone are required.");
        }
        if (wardService.wardExistsForOtherId(targetWardNo, targetWardZone, wardId)) {
            return badRequest("Ward already exists for wardNo " + targetWardNo + " and wardZone " + targetWardZone + ".");
        }

        Ward patch = new Ward();
        patch.setWardNo(request.wardNo());
        patch.setWardZone(request.wardZone());
        if (request.name() != null) {
            patch.setName(isBlank(request.name()) ? null : request.name().trim());
        }

        Ward saved = wardService.updateWard(wardId, patch).orElse(null);
        if (saved == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ward not found."));
        }

        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWard(@PathVariable("id") Long wardId) {
        boolean deleted = wardService.deleteWard(wardId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ward not found."));
        }
        return ResponseEntity.ok(Map.of("message", "Ward deleted successfully."));
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

    public record UpdateWardRequest(Integer wardNo, String wardZone, String name) {
    }

    public record WardResponse(Long id, Integer wardNo, String wardZone, String name, LocalDateTime createdAt) {
    }
}
