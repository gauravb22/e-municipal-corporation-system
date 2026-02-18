package com.emunicipal.controller;

import com.emunicipal.entity.StaffUser;
import com.emunicipal.entity.WorksYojnaItem;
import com.emunicipal.repository.WorksYojnaItemRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class WorksYojnaController {

    @Autowired
    private WorksYojnaItemRepository worksYojnaItemRepository;

    @GetMapping("/works-yojna")
    public String worksYojnaPage(Model model) {
        model.addAttribute("workItems", worksYojnaItemRepository.findBySectionTypeOrderByCreatedAtDesc("WORK"));
        model.addAttribute("yojnaItems", worksYojnaItemRepository.findBySectionTypeOrderByCreatedAtDesc("YOJNA"));
        return "works-yojna";
    }

    @GetMapping("/admin/works-yojna")
    public String adminWorksYojna(HttpSession session, Model model) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        model.addAttribute("staffUser", staffUser);
        model.addAttribute("workItems", worksYojnaItemRepository.findBySectionTypeOrderByCreatedAtDesc("WORK"));
        model.addAttribute("yojnaItems", worksYojnaItemRepository.findBySectionTypeOrderByCreatedAtDesc("YOJNA"));
        return "admin-works-yojna";
    }

    @PostMapping("/admin/works-yojna/create")
    public String createItem(@RequestParam("sectionType") String sectionType,
                             @RequestParam("title") String title,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "statusLabel", required = false) String statusLabel,
                             @RequestParam(value = "metaOne", required = false) String metaOne,
                             @RequestParam(value = "metaTwo", required = false) String metaTwo,
                             @RequestParam(value = "actionText", required = false) String actionText,
                             @RequestParam(value = "actionUrl", required = false) String actionUrl,
                             HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        if (title == null || title.isBlank()) {
            return "redirect:/admin/works-yojna?error=title";
        }

        WorksYojnaItem item = new WorksYojnaItem();
        item.setSectionType(normalizeSectionType(sectionType));
        item.setTitle(title.trim());
        item.setDescription(normalize(description, 4000));
        item.setStatusLabel(normalize(statusLabel, 40));
        item.setMetaOne(normalize(metaOne, 250));
        item.setMetaTwo(normalize(metaTwo, 250));
        item.setActionText(normalize(actionText, 120));
        item.setActionUrl(normalize(actionUrl, 500));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setCreatedBy(staffUser.getUsername());
        item.setUpdatedBy(staffUser.getUsername());
        worksYojnaItemRepository.save(item);
        return "redirect:/admin/works-yojna?created=1";
    }

    @PostMapping("/admin/works-yojna/{id}/update")
    public String updateItem(@PathVariable("id") Long itemId,
                             @RequestParam("sectionType") String sectionType,
                             @RequestParam("title") String title,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "statusLabel", required = false) String statusLabel,
                             @RequestParam(value = "metaOne", required = false) String metaOne,
                             @RequestParam(value = "metaTwo", required = false) String metaTwo,
                             @RequestParam(value = "actionText", required = false) String actionText,
                             @RequestParam(value = "actionUrl", required = false) String actionUrl,
                             HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        if (title == null || title.isBlank()) {
            return "redirect:/admin/works-yojna?error=title";
        }

        WorksYojnaItem item = worksYojnaItemRepository.findById(itemId).orElse(null);
        if (item == null) {
            return "redirect:/admin/works-yojna?error=notfound";
        }

        item.setSectionType(normalizeSectionType(sectionType));
        item.setTitle(title.trim());
        item.setDescription(normalize(description, 4000));
        item.setStatusLabel(normalize(statusLabel, 40));
        item.setMetaOne(normalize(metaOne, 250));
        item.setMetaTwo(normalize(metaTwo, 250));
        item.setActionText(normalize(actionText, 120));
        item.setActionUrl(normalize(actionUrl, 500));
        item.setUpdatedAt(LocalDateTime.now());
        item.setUpdatedBy(staffUser.getUsername());
        worksYojnaItemRepository.save(item);
        return "redirect:/admin/works-yojna?updated=1";
    }

    @PostMapping("/admin/works-yojna/{id}/delete")
    public String deleteItem(@PathVariable("id") Long itemId, HttpSession session) {
        StaffUser staffUser = (StaffUser) session.getAttribute("staffUser");
        if (staffUser == null || !"ADMIN".equalsIgnoreCase(staffUser.getRole())) {
            return "redirect:/admin-login";
        }

        if (worksYojnaItemRepository.existsById(itemId)) {
            worksYojnaItemRepository.deleteById(itemId);
        }
        return "redirect:/admin/works-yojna?deleted=1";
    }

    private String normalizeSectionType(String sectionType) {
        if (sectionType == null || sectionType.isBlank()) {
            return "WORK";
        }
        String normalized = sectionType.trim().toUpperCase();
        if ("YOJNA".equals(normalized)) {
            return "YOJNA";
        }
        return "WORK";
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }
}
