package com.infopouch.api.modules.users.infrastructure;

import com.infopouch.api.modules.users.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfileRepository extends JpaRepository<Profile, String> {}
