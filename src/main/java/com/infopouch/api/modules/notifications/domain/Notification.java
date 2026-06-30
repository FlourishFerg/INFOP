package com.infopouch.api.modules.notifications.domain;

import com.infopouch.api.common.util.IdGenerator;
import com.infopouch.api.modules.users.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private NotificationType type;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (this.id == null) {
      this.id = IdGenerator.generate("ntf");
    }
    this.createdAt = LocalDateTime.now();
  }
}
