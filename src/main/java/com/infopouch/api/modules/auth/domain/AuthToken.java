package com.infopouch.api.modules.auth.domain;

import com.infopouch.api.common.util.IdGenerator;
import com.infopouch.api.modules.users.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "auth_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {

  @Id private String id;

  @Column(name = "token_value", unique = true, nullable = false, length = 500)
  private String tokenValue;

  @Column(name = "token_type", nullable = false)
  private String tokenType; // "VERIFICATION", "PASSWORD_RESET", "REFRESH"

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "expiry_date", nullable = false)
  private LocalDateTime expiryDate;

  @Column(name = "is_revoked", nullable = false)
  private boolean isRevoked = false;

  @PrePersist
  protected void onCreate() {
    if (this.id == null) {
      this.id = IdGenerator.generate("tok");
    }
  }
}
