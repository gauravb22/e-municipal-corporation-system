package com.emunicipal.controller;

import com.emunicipal.entity.Notification;
import com.emunicipal.entity.User;
import com.emunicipal.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/citizen/notifications/{id}/open")
    public String openCitizenNotification(@PathVariable("id") Long notificationId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Notification> notificationOpt = notificationService.getNotificationForUser(notificationId, user.getId());
        if (notificationOpt.isEmpty()) {
            return "redirect:/dashboard";
        }

        Notification notification = notificationOpt.get();
        notificationService.markAsRead(notification.getId(), user.getId());

        String actionUrl = notification.getActionUrl();
        if (actionUrl == null || actionUrl.isBlank() || !actionUrl.startsWith("/")) {
            return "redirect:/dashboard";
        }
        return "redirect:" + actionUrl;
    }

    @PostMapping("/citizen/notifications/read-all")
    public String markAllCitizenNotificationsRead(
            @RequestParam(value = "redirect", required = false) String redirectPath,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        notificationService.markAllAsRead(user.getId());

        if (redirectPath == null || redirectPath.isBlank() || !redirectPath.startsWith("/")) {
            return "redirect:/dashboard";
        }
        return "redirect:" + redirectPath;
    }
}
