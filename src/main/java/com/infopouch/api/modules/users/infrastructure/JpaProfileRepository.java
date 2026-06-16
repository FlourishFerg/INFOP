package com.infopouch.api.modules.users.infrastructure;

import com.infopouch.api.modules.users.domain.Profile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfileRepository extends JpaRepository<Profile, String> {
  Optional<Profile> findByUserId(String userId);

  Optional<Profile> findByUserEmail(String email);
}
