package com.infopouch.api.modules.users.domain;

import jakarta.persistence.*;

public enum Role {
  ADMIN,
  RESEARCHER,
  STUDENT,
  LECTURER,
  PROFESSIONAL,
  GUEST
}
