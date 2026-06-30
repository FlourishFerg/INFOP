package com.infopouch.api.modules.notifications.infrastructure;

import com.infopouch.api.modules.notifications.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationRepository extends JpaRepository<Notification, String> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
}
