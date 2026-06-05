package com.infopouch.api.modules.auth.presentation.dto;

import com.infopouch.api.modules.users.domain.MembershipTier;
import com.infopouch.api.modules.users.domain.ProfileType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String fullName,
    String phoneNumber,
    String country,
    String geopoliticalZone,
    String state,
    String city,
    String profession,
    @NotNull ProfileType profileType,
    @NotNull MembershipTier membershipTier,
    String academicQualification,
    String gender,
    LocalDate dateOfBirth) {}
