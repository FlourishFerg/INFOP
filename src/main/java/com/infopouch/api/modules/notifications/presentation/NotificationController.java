package com.infopouch.api.modules.notifications.presentation;

import com.infopouch.api.modules.auth.presentation.dto.ApiResponse;
import com.infopouch.api.modules.notifications.application.NotificationUseCase;
import com.infopouch.api.modules.notifications.presentation.dto.NotificationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationUseCase notificationUseCase;

  @GetMapping
  public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
      @AuthenticationPrincipal UserDetails userDetails) {
    List<NotificationResponse> data =
        notificationUseCase.getUserNotifications(userDetails.getUsername());
    return ResponseEntity.ok(ApiResponse.success("Notifications loaded successfully.", data));
  }

  @PatchMapping("/{id}/read")
  public ResponseEntity<ApiResponse<String>> markAsRead(
      @PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
    notificationUseCase.markAsRead(id, userDetails.getUsername());
    return ResponseEntity.ok(ApiResponse.success("Notification marked as read."));
  }
}
