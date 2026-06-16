package com.infopouch.api.modules.users.presentation.dto;

import com.infopouch.api.modules.users.domain.MembershipTier;
import com.infopouch.api.modules.users.domain.ProfileType;
import com.infopouch.api.modules.users.domain.Role;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProfileResponse(
    String id,
    String userId,
    String email,
    String fullName,
    Role role,
    ProfileType profileType,
    MembershipTier membershipTier,
    String phoneNumber,
    String country,
    String geopoliticalZone,
    String state,
    String city,
    String profession,
    String academicQualification,
    String gender,
    LocalDate dateOfBirth,
    boolean isVerified,
    boolean onboardingCompleted,
    LocalDateTime createdAt) {}
