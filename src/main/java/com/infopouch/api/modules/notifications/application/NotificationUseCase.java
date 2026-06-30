package com.infopouch.api.modules.notifications.application;

import com.infopouch.api.modules.notifications.domain.NotificationType;
import com.infopouch.api.modules.notifications.presentation.dto.NotificationResponse;
import com.infopouch.api.modules.users.domain.User;
import java.util.List;

public interface NotificationUseCase {
  void createNotification(User user, NotificationType type, String title, String message);

  List<NotificationResponse> getUserNotifications(String currentUserEmail);

  void markAsRead(String id, String currentUserEmail);
}
