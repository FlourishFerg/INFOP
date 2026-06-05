package com.infopouch.api.modules.users.infrastructure;

import com.infopouch.api.modules.auth.domain.AuthToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAuthTokenRepository extends JpaRepository<AuthToken, String> {
  Optional<AuthToken> findByTokenValue(String tokenValue);

  void deleteByTokenValue(String tokenValue);
}
