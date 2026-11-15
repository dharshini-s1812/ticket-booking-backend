package com.TicketBooking.Service;

import com.TicketBooking.DTO.Response.NotificationResponse;
import com.TicketBooking.Exception.ResourceNotFoundException;
import com.TicketBooking.Model.Notification;
import com.TicketBooking.Model.NotificationType;
import com.TicketBooking.Model.User;
import com.TicketBooking.Repository.NotificationRepository;
import com.TicketBooking.Repository.RoleRepository;
import com.TicketBooking.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void notifyUser(User user, NotificationType type, String title, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void notifyAllUsers(NotificationType type, String title, String message) {
        List<User> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        List<Notification> notifications = users.stream()
                .map(u -> Notification.builder()
                        .user(u)
                        .type(type)
                        .title(title)
                        .message(message)
                        .createdAt(now)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message) {
        roleRepository.findByName("ROLE_ADMIN").ifPresent(adminRole -> {
            List<User> admins = userRepository.findByRoleId(adminRole.getId());
            LocalDateTime now = LocalDateTime.now();
            List<Notification> notifications = admins.stream()
                    .map(u -> Notification.builder()
                            .user(u)
                            .type(type)
                            .title(title)
                            .message(message)
                            .createdAt(now)
                            .build())
                    .toList();
            notificationRepository.saveAll(notifications);
        });
    }

    @Override
    @Transactional
    public void notifyUserThrottled(User user, NotificationType type, String title, String message, int throttleHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(throttleHours);
        boolean sentRecently = notificationRepository
                .existsByUserIdAndTypeAndCreatedAtAfter(user.getId(), type, cutoff);
        if (sentRecently) {
            return;
        }
        notifyUser(user, type, title, message);
    }

    @Override
    public List<NotificationResponse> getMyNotifications(String userEmail) {
        User user = getUser(userEmail);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    public long getUnreadCount(String userEmail) {
        User user = getUser(userEmail);
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Override
    @Transactional
    public void markRead(String userEmail, Long notificationId) {
        User user = getUser(userEmail);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead(String userEmail) {
        User user = getUser(userEmail);
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).toList();
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
