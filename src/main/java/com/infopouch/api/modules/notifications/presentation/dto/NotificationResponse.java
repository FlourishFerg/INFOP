package com.infopouch.api.modules.notifications.presentation.dto;

import com.infopouch.api.modules.notifications.domain.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
    String id,
    NotificationType type,
    String title,
    String message,
    boolean read,
    LocalDateTime createdAt) {}
