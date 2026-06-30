package com.infopouch.api.modules.research.domain;

import com.infopouch.api.common.util.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "research_share_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchShareToken {

  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "research_id", nullable = false)
  private ResearchPaper researchPaper;

  @Column(nullable = false, unique = true, length = 100)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (this.id == null) {
      this.id = IdGenerator.generate("shr");
    }
    if (this.token == null) {
      this.token = UUID.randomUUID().toString().replace("-", "");
    }
    this.createdAt = LocalDateTime.now();
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }
}
