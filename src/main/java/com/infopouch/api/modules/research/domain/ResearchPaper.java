package com.infopouch.api.modules.research.domain;

import com.infopouch.api.common.util.IdGenerator;
import com.infopouch.api.modules.users.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "research_papers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchPaper {

  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String abstractText;

  @Column(name = "file_key", nullable = false, length = 500)
  private String fileKey;

  @Column(name = "file_url", nullable = false, length = 1000)
  private String fileUrl;

  @Column(name = "file_size_bytes", nullable = false)
  private Long fileSizeBytes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ResearchStatus status;

  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;

  @OneToMany(mappedBy = "researchPaper", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ResearchKeyword> keywords = new ArrayList<>();

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    if (this.id == null) {
      this.id = IdGenerator.generate("res");
    }
    this.status = ResearchStatus.PENDING;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
