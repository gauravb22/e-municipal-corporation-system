package com.emunicipal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import com.emunicipal.entity.WardWork;
import com.emunicipal.entity.User;
import com.emunicipal.entity.StaffUser;
import com.emunicipal.entity.WardWorkLike;
import com.emunicipal.entity.WardWorkComment;
import com.emunicipal.entity.WardWorkRating;
import com.emunicipal.repository.WardWorkRepository;
import com.emunicipal.repository.WardWorkLikeRepository;
import com.emunicipal.repository.WardWorkCommentRepository;
import com.emunicipal.repository.WardWorkRatingRepository;
import com.emunicipal.repository.UserRepository;
import com.emunicipal.repository.StaffUserRepository;
import com.emunicipal.util.ImageFormatValidator;

import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class WorkController {

    @Autowired
    private WardWorkRepository repo;

    @Autowired
    private WardWorkLikeRepository likeRepo;

    @Autowired
    private WardWorkCommentRepository commentRepo;

    @Autowired
    private WardWorkRatingRepository ratingRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffUserRepository staffUserRepository;

    /*
     ===============================
     LOAD WARD WORK FEED
     ===============================
    */

    @GetMapping("/ward-works")
    public String wardWorksPage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");

        if (user == null && staffUser == null) {
            return "redirect:/login";
        }

        // Prefer ward view if staff session exists
        if (staffUser != null && "WARD".equalsIgnoreCase(staffUser.getRole())) {
            user = null;
        } else if (user != null) {
            staffUser = null;
        }
        Integer wardNo = staffUser != null ? staffUser.getWardNo() : null;
        List<WardWork> posts = (wardNo != null)
                ? repo.findByWardNoOrderByCreatedAtDesc(wardNo)
                : repo.findAllByOrderByCreatedAtDesc();

        Map<Long, Long> likeCounts = new HashMap<>();
        Map<Long, List<WardWorkCommentView>> commentsMap = new HashMap<>();
        Map<Long, Integer> userRatings = new HashMap<>();
        Map<Long, Boolean> userLiked = new HashMap<>();
        Map<Long, Double> avgRatings = new HashMap<>();
        Map<Long, String> postPhotos = new HashMap<>();
        Map<Long, String> postOwnerNames = new HashMap<>();

        Long viewerId = user != null ? user.getId() : null;

        for (WardWork post : posts) {
            likeCounts.put(post.getId(), likeRepo.countByWorkId(post.getId()));

            List<WardWorkComment> comments = commentRepo.findByWorkIdOrderByCreatedAtDesc(post.getId());
            List<WardWorkCommentView> commentViews = new ArrayList<>();
            for (WardWorkComment c : comments) {
                User commentUser = userRepository.findById(c.getUserId()).orElse(null);
                String name = commentUser != null ? commentUser.getFullName() : "Citizen";
                commentViews.add(new WardWorkCommentView(name, c.getText(), c.getCreatedAt()));
            }
            commentsMap.put(post.getId(), commentViews);

            Double avg = ratingRepo.getAverageRatingByWorkId(post.getId());
            avgRatings.put(post.getId(), avg != null ? avg : 0.0);

            if (post.getDoneBy() != null) {
                StaffUser postOwner = staffUserRepository.findByUsername(post.getDoneBy());
                if (postOwner != null && postOwner.getPhotoBase64() != null && !postOwner.getPhotoBase64().isBlank()) {
                    postPhotos.put(post.getId(), postOwner.getPhotoBase64());
                }
                if (post.getDoneByName() != null && !post.getDoneByName().isBlank()) {
                    postOwnerNames.put(post.getId(), post.getDoneByName());
                } else if (postOwner != null && postOwner.getFullName() != null && !postOwner.getFullName().isBlank()) {
                    postOwnerNames.put(post.getId(), postOwner.getFullName());
                }
            }

            if (!postOwnerNames.containsKey(post.getId())) {
                postOwnerNames.put(post.getId(), "Ward Member");
            }

            if (viewerId != null) {
                userLiked.put(post.getId(), likeRepo.existsByWorkIdAndUserId(post.getId(), viewerId));
                WardWorkRating rating = ratingRepo.findByWorkIdAndUserId(post.getId(), viewerId);
                if (rating != null) {
                    userRatings.put(post.getId(), rating.getRating());
                }
            }
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("likeCounts", likeCounts);
        model.addAttribute("commentsMap", commentsMap);
        model.addAttribute("userRatings", userRatings);
        model.addAttribute("userLiked", userLiked);
        model.addAttribute("avgRatings", avgRatings);
        model.addAttribute("postPhotos", postPhotos);
        model.addAttribute("postOwnerNames", postOwnerNames);
        return staffUser != null ? "ward-works" : "ward-works-citizen";
    }

    @GetMapping("/ward-works-citizen")
    public String wardWorksCitizenRedirect() {
        return "redirect:/ward-works";
    }

    @GetMapping("/citizen/ward-works")
    public String citizenWardWorksRedirect() {
        return "redirect:/ward-works";
    }

    /*
     ===============================
     API - GET USER WARD WORKS
     ===============================
    */

    @GetMapping("/api/ward-works")
    @ResponseBody
    public List<WardWork> getWorks(HttpSession session) {

        User user = (User) session.getAttribute("user");
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");

        if (user == null && staffUser == null) {
            return List.of();
        }

        if (staffUser != null) {
            Integer wardNo = staffUser.getWardNo();
            if (wardNo == null) {
                return List.of();
            }
            return repo.findByWardNoOrderByCreatedAtDesc(wardNo);
        }

        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/ward-works/create")
    public String createWardWork(@RequestParam("title") String title,
                                 @RequestParam("description") String description,
                                 @RequestParam("address") String address,
                                 @RequestParam(value = "wardNo", required = false) Integer wardNo,
                                 @RequestParam(value = "wardZone", required = false) String wardZone,
                                 @RequestParam("workDoneDate") String workDoneDate,
                                 @RequestParam("imageBase64") String imageBase64,
                                 HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }
        if (!ImageFormatValidator.isJpgDataUrl(imageBase64)) {
            return "redirect:/ward-works";
        }

        WardWork post = new WardWork();
        post.setTitle(title);
        post.setDescription(description);
        post.setAddress(address);
        post.setWardNo(wardNo != null ? wardNo : staffUser.getWardNo());
        post.setWardZone(wardZone != null && !wardZone.isBlank() ? wardZone.trim() : staffUser.getWardZone());
        post.setDoneBy(staffUser.getUsername());
        post.setDoneByName(staffUser.getFullName() != null && !staffUser.getFullName().isBlank() ? staffUser.getFullName().trim() : "Ward Member");
        post.setImageBase64(imageBase64);
        post.setCreatedAt(LocalDateTime.now());
        if (workDoneDate != null && !workDoneDate.isBlank()) {
            post.setWorkDoneDate(LocalDate.parse(workDoneDate));
        }
        repo.save(post);
        return "redirect:/ward-works";
    }

    @PostMapping("/ward-works/{id}/like")
    public String likeWardWork(@PathVariable("id") Long workId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (!likeRepo.existsByWorkIdAndUserId(workId, user.getId())) {
            WardWorkLike like = new WardWorkLike();
            like.setWorkId(workId);
            like.setUserId(user.getId());
            likeRepo.save(like);
        }
        return "redirect:/ward-works";
    }

    @PostMapping("/ward-works/{id}/comment")
    public String commentWardWork(@PathVariable("id") Long workId,
                                  @RequestParam("comment") String comment,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (comment != null && !comment.isBlank()) {
            WardWorkComment c = new WardWorkComment();
            c.setWorkId(workId);
            c.setUserId(user.getId());
            c.setText(comment.trim());
            c.setCreatedAt(LocalDateTime.now());
            commentRepo.save(c);
        }
        return "redirect:/ward-works";
    }

    @PostMapping("/ward-works/{id}/rate")
    public String rateWardWork(@PathVariable("id") Long workId,
                               @RequestParam("rating") Integer rating,
                               HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (rating == null || rating < 1 || rating > 5) {
            return "redirect:/ward-works";
        }
        WardWorkRating existing = ratingRepo.findByWorkIdAndUserId(workId, user.getId());
        if (existing != null) {
            return "redirect:/ward-works";
        }
        WardWorkRating r = new WardWorkRating();
        r.setWorkId(workId);
        r.setUserId(user.getId());
        r.setRating(rating);
        ratingRepo.save(r);

        Double avg = ratingRepo.getAverageRatingByWorkId(workId);
        repo.findById(workId).ifPresent(w -> {
            w.setRating(avg != null ? avg : 0.0);
            repo.save(w);
        });

        return "redirect:/ward-works";
    }

    @PostMapping("/ward-works/{id}/edit")
    public String editWardWork(@PathVariable("id") Long workId,
                               @RequestParam("title") String title,
                               @RequestParam("description") String description,
                               HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        repo.findById(workId).ifPresent(post -> {
            if (staffUser.getUsername().equals(post.getDoneBy())) {
                post.setTitle(title);
                post.setDescription(description);
                repo.save(post);
            }
        });
        return "redirect:/ward-works";
    }

    @PostMapping("/ward-works/{id}/delete")
    public String deleteWardWork(@PathVariable("id") Long workId, HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"WARD".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/ward-login";
        }

        repo.findById(workId).ifPresent(post -> {
            if (staffUser.getUsername().equals(post.getDoneBy())) {
                repo.deleteById(workId);
            }
        });
        return "redirect:/ward-works";
    }

    public static class WardWorkCommentView {
        private final String name;
        private final String text;
        private final LocalDateTime createdAt;

        public WardWorkCommentView(String name, String text, LocalDateTime createdAt) {
            this.name = name;
            this.text = text;
            this.createdAt = createdAt;
        }

        public String getName() { return name; }
        public String getText() { return text; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
