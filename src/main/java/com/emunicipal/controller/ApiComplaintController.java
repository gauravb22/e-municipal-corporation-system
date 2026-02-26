package com.emunicipal.controller;

import com.emunicipal.entity.Complaint;
import com.emunicipal.entity.User;
import com.emunicipal.entity.Ward;
import com.emunicipal.repository.ComplaintRepository;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.service.ComplaintService;
import com.emunicipal.service.WardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/complaints")
public class ApiComplaintController {

    private final ComplaintRepository complaintRepository;
    private final ComplaintService complaintService;
    private final UserRepository userRepository;
    private final WardService wardService;

    public ApiComplaintController(ComplaintRepository complaintRepository,
                                  ComplaintService complaintService,
                                  UserRepository userRepository,
                                  WardService wardService) {
        this.complaintRepository = complaintRepository;
        this.complaintService = complaintService;
        this.userRepository = userRepository;
        this.wardService = wardService;
    }

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> getComplaints(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "wardNo", required = false) Integer wardNo,
            @RequestParam(value = "status", required = false) String status) {

        List<Complaint> complaints = complaintRepository.findAll();
        complaintService.refreshOverdueForComplaints(complaints);

        Stream<Complaint> stream = complaints.stream();
        if (userId != null) {
            stream = stream.filter(c -> userId.equals(c.getUserId()));
        }
        if (wardNo != null) {
            stream = stream.filter(c -> wardNo.equals(c.getWardNo()));
        }
        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toLowerCase();
            stream = stream.filter(c -> c.getStatus() != null && normalizedStatus.equals(c.getStatus().trim().toLowerCase()));
        }

        List<ComplaintResponse> payload = stream
                .sorted(Comparator.comparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(payload);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaintById(@PathVariable("id") Long complaintId) {
        Complaint complaint = complaintService.getComplaintById(complaintId).orElse(null);
        if (complaint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Complaint not found."));
        }
        return ResponseEntity.ok(toResponse(complaint));
    }

    @PostMapping
    public ResponseEntity<?> createComplaint(@RequestBody CreateComplaintRequest request) {
        if (request == null) {
            return badRequest("Request body is required.");
        }
        if (request.userId() == null) {
            return badRequest("userId is required.");
        }
        if (isBlank(request.complaintType())) {
            return badRequest("complaintType is required.");
        }
        if (isBlank(request.location())) {
            return badRequest("location is required.");
        }
        if (isBlank(request.houseNo())) {
            return badRequest("houseNo is required.");
        }

        User user = userRepository.findById(request.userId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found for userId: " + request.userId()));
        }

        LocalDate parsedNotComingFromDate = null;
        if (!isBlank(request.notComingFromDate())) {
            try {
                parsedNotComingFromDate = LocalDate.parse(request.notComingFromDate().trim());
            } catch (DateTimeParseException ex) {
                return badRequest("notComingFromDate must be in ISO format yyyy-MM-dd.");
            }
        }

        String resolvedPhotoTimestamp = isBlank(request.photoTimestamp())
                ? LocalDateTime.now().toString()
                : request.photoTimestamp().trim();
        String resolvedPhotoLocation = isBlank(request.photoLocation())
                ? "Location not provided"
                : request.photoLocation().trim();
        String resolvedPhotoLatitude = isBlank(request.photoLatitude())
                ? "0"
                : request.photoLatitude().trim();
        String resolvedPhotoLongitude = isBlank(request.photoLongitude())
                ? "0"
                : request.photoLongitude().trim();

        Complaint complaint = new Complaint(
                user.getId(),
                request.complaintType().trim(),
                request.location().trim(),
                request.houseNo().trim(),
                blankToNull(request.description()),
                resolvedPhotoTimestamp,
                resolvedPhotoLocation,
                resolvedPhotoLatitude,
                resolvedPhotoLongitude,
                blankToNull(request.photoBase64())
        );
        complaint.setNotComingFromDate(parsedNotComingFromDate);

        Integer resolvedWardNo = request.wardNo() != null ? request.wardNo() : user.getWardNo();
        String resolvedWardZone = !isBlank(request.wardZone()) ? request.wardZone().trim() : user.getWardZone();

        complaint.setWardNo(resolvedWardNo);
        complaint.setWardZone(resolvedWardZone);

        Ward ward = wardService.resolveWard(resolvedWardNo, resolvedWardZone);
        if (ward != null) {
            complaint.setWardId(ward.getId());
            complaint.setWardNo(ward.getWardNo());
            complaint.setWardZone(ward.getWardZone());
        }

        Complaint saved = complaintService.saveComplaint(complaint);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        return new ComplaintResponse(
                complaint.getId(),
                complaint.getUserId(),
                complaint.getComplaintType(),
                complaint.getLocation(),
                complaint.getHouseNo(),
                complaint.getWardNo(),
                complaint.getWardZone(),
                complaint.getWardId(),
                complaint.getDescription(),
                complaint.getNotComingFromDate(),
                complaint.getPhotoTimestamp(),
                complaint.getPhotoLocation(),
                complaint.getPhotoLatitude(),
                complaint.getPhotoLongitude(),
                complaint.getPhotoPath(),
                complaint.getDonePhotoTimestamp(),
                complaint.getDonePhotoLocation(),
                complaint.getDonePhotoLatitude(),
                complaint.getDonePhotoLongitude(),
                complaint.getDonePhotoPath(),
                complaint.getStatus(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                complaint.getFeedbackSubmitted(),
                complaint.getFeedbackDescription(),
                complaint.getFeedbackRating(),
                complaint.getFeedbackSolved(),
                complaint.getFeedbackSolvedBy(),
                complaint.getFeedbackSubmittedAt()
        );
    }

    public record CreateComplaintRequest(
            Long userId,
            String complaintType,
            String location,
            String houseNo,
            String description,
            Integer wardNo,
            String wardZone,
            String photoTimestamp,
            String photoLocation,
            String photoLatitude,
            String photoLongitude,
            String photoBase64,
            String notComingFromDate
    ) {
    }

    public record ComplaintResponse(
            Long id,
            Long userId,
            String complaintType,
            String location,
            String houseNo,
            Integer wardNo,
            String wardZone,
            Long wardId,
            String description,
            LocalDate notComingFromDate,
            String photoTimestamp,
            String photoLocation,
            String photoLatitude,
            String photoLongitude,
            String photoPath,
            String donePhotoTimestamp,
            String donePhotoLocation,
            String donePhotoLatitude,
            String donePhotoLongitude,
            String donePhotoPath,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Boolean feedbackSubmitted,
            String feedbackDescription,
            Integer feedbackRating,
            Boolean feedbackSolved,
            String feedbackSolvedBy,
            LocalDateTime feedbackSubmittedAt
    ) {
    }
}
