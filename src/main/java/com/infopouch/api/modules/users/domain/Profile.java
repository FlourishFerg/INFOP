package com.infopouch.api.modules.users.domain;

import com.infopouch.api.common.util.IdGenerator;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private String id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", unique = true, nullable = false)
  private User user;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "phone_number")
  private String phoneNumber;

  private String country;

  @Column(name = "geopolitical_zone")
  private String geopoliticalZone;

  private String state;
  private String city;
  private String profession;

  @Enumerated(EnumType.STRING)
  @Column(name = "profile_type", nullable = false)
  private ProfileType profileType;

  @Enumerated(EnumType.STRING)
  @Column(name = "membership_type")
  private MembershipTier membershipTier;

  @Column(name = "academic_qualification")
  private String academicQualification;

  private String gender;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(name = "onboarding_completed", nullable = false)
  private boolean onboardingCompleted;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    if (this.id == null) {
      this.id = IdGenerator.generate("prof");
    }
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
