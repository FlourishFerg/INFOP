package com.infopouch.api.modules.notifications.application;

import com.infopouch.api.common.exception.ResourceNotFoundException;
import com.infopouch.api.modules.notifications.domain.Notification;
import com.infopouch.api.modules.notifications.domain.NotificationType;
import com.infopouch.api.modules.notifications.infrastructure.JpaNotificationRepository;
import com.infopouch.api.modules.notifications.presentation.dto.NotificationResponse;
import com.infopouch.api.modules.users.domain.User;
import com.infopouch.api.modules.users.infrastructure.JpaUserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationUseCaseImpl implements NotificationUseCase {

  private final JpaNotificationRepository notificationRepository;
  private final JpaUserRepository userRepository;

  @Override
  @Transactional
  public void createNotification(User user, NotificationType type, String title, String message) {
    Notification notification =
        Notification.builder().user(user).type(type).title(title).message(message).build();
    notificationRepository.save(notification);
  }

  @Override
  @Transactional(readOnly = true)
  public List<NotificationResponse> getUserNotifications(String currentUserEmail) {
    User user =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new IllegalArgumentException("User principal not found."));

    return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
        .map(
            n ->
                new NotificationResponse(
                    n.getId(),
                    n.getType(),
                    n.getTitle(),
                    n.getMessage(),
                    n.isRead(),
                    n.getCreatedAt()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void markAsRead(String id, String currentUserEmail) {
    Notification notification =
        notificationRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

    if (!notification.getUser().getEmail().equals(currentUserEmail)) {
      throw new IllegalStateException("Unauthorized notification access.");
    }

    notification.setRead(true);
    notificationRepository.save(notification);
  }
}
