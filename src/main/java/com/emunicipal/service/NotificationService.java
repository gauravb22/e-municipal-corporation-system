package com.emunicipal.service;

import com.emunicipal.entity.Complaint;
import com.emunicipal.entity.Notification;
import com.emunicipal.entity.Notice;
import com.emunicipal.entity.User;
import com.emunicipal.repository.NotificationRepository;
import com.emunicipal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    public Notification createNotification(Long userId,
                                           String title,
                                           String message,
                                           String type,
                                           String referenceType,
                                           Long referenceId,
                                           String actionUrl) {
        if (userId == null || title == null || title.isBlank() || message == null || message.isBlank() || type == null || type.isBlank()) {
            return null;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        notification.setType(type.trim());
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setActionUrl(actionUrl != null && !actionUrl.isBlank() ? actionUrl.trim() : "/dashboard");
        notification.setReadFlag(false);
        notification.setReadAt(null);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public void notifyComplaintApproved(Complaint complaint) {
        if (complaint == null || complaint.getId() == null || complaint.getUserId() == null) {
            return;
        }

        createNotification(
                complaint.getUserId(),
                "Complaint Approved",
                "Your complaint #" + complaint.getId() + " has been approved by the ward office.",
                "COMPLAINT_APPROVED",
                "COMPLAINT",
                complaint.getId(),
                "/complaint-status"
        );
    }

    public void notifyComplaintCompleted(Complaint complaint) {
        if (complaint == null || complaint.getId() == null || complaint.getUserId() == null) {
            return;
        }

        createNotification(
                complaint.getUserId(),
                "Work Completed",
                "Work for your complaint #" + complaint.getId() + " is marked as completed.",
                "COMPLAINT_COMPLETED",
                "COMPLAINT",
                complaint.getId(),
                "/complaint-status"
        );
    }

    public void notifyCitizensForAdminNotice(Notice notice) {
        if (notice == null || notice.getId() == null || notice.getMessage() == null || notice.getMessage().isBlank()) {
            return;
        }

        List<User> citizens = userRepository.findByActiveTrueOrActiveIsNull();
        for (User citizen : citizens) {
            if (citizen.getId() == null) {
                continue;
            }
            createNotification(
                    citizen.getId(),
                    "New Admin Notice",
                    notice.getMessage(),
                    "ADMIN_NOTICE",
                    "NOTICE",
                    notice.getId(),
                    "/dashboard"
            );
        }
    }

    public List<Notification> getRecentForUser(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        if (limit <= 0 || limit >= 10) {
            return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(limit)
                .toList();
    }

    public long countUnread(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return notificationRepository.countByUserIdAndReadFlagFalse(userId);
    }

    public Optional<Notification> getNotificationForUser(Long notificationId, Long userId) {
        if (notificationId == null || userId == null) {
            return Optional.empty();
        }
        return notificationRepository.findByIdAndUserId(notificationId, userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        if (notificationId == null || userId == null) {
            return;
        }
        notificationRepository.findByIdAndUserId(notificationId, userId).ifPresent(notification -> {
            if (Boolean.TRUE.equals(notification.getReadFlag())) {
                return;
            }
            notification.setReadFlag(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    public void markAllAsRead(Long userId) {
        if (userId == null) {
            return;
        }
        List<Notification> unread = notificationRepository.findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(userId);
        if (unread.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : unread) {
            notification.setReadFlag(true);
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
    }
}
