package com.TicketBooking.Service;

import com.TicketBooking.DTO.Response.NotificationResponse;
import com.TicketBooking.Model.NotificationType;
import com.TicketBooking.Model.User;

import java.util.List;

public interface NotificationService {

    void notifyUser(User user, NotificationType type, String title, String message);

    // Fans a notification out to every user — used for "new movie added".
    // Naturally infrequent (movies aren't added often), so no throttling
    // needed on top of that.
    void notifyAllUsers(NotificationType type, String title, String message);

    // Sends to every user with the ADMIN role — used for operational
    // notices (a run ending soon and needing an extend/end decision) that
    // customers shouldn't see.
    void notifyAdmins(NotificationType type, String title, String message);

    // Only sends if this user hasn't gotten one of this type in the last
    // `throttleHours` — used for LOGIN so a user who logs in five times a
    // day doesn't get five "welcome back" notifications.
    void notifyUserThrottled(User user, NotificationType type, String title, String message, int throttleHours);

    List<NotificationResponse> getMyNotifications(String userEmail);

    long getUnreadCount(String userEmail);

    void markRead(String userEmail, Long notificationId);

    void markAllRead(String userEmail);
}
