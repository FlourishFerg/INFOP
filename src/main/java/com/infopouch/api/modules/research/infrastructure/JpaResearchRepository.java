package com.infopouch.api.modules.research.infrastructure;

import com.infopouch.api.modules.research.domain.ResearchPaper;
import com.infopouch.api.modules.research.domain.ResearchStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaResearchRepository extends JpaRepository<ResearchPaper, String> {
  List<ResearchPaper> findByUserId(String userId);

  List<ResearchPaper> findByStatus(ResearchStatus status);
}
