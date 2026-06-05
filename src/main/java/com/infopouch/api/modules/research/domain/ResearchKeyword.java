package com.infopouch.api.modules.research.domain;

import com.infopouch.api.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "research_keywords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchKeyword {

  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "research_id", nullable = false)
  private ResearchPaper researchPaper;

  @Column(nullable = false, length = 100)
  private String keyword;

  @PrePersist
  protected void onCreate() {
    if (this.id == null) {
      this.id = IdGenerator.generate("key");
    }
  }
}
