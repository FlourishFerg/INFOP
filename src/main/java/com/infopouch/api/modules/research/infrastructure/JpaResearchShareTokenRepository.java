package com.infopouch.api.modules.research.infrastructure;

import com.infopouch.api.modules.research.domain.ResearchShareToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaResearchShareTokenRepository extends JpaRepository<ResearchShareToken, String> {
  Optional<ResearchShareToken> findByToken(String token);
}
