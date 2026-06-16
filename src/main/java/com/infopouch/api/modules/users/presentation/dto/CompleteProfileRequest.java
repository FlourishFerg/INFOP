package com.infopouch.api.modules.users.presentation.dto;

import java.time.LocalDate;

public record CompleteProfileRequest(
    String phoneNumber,
    String country,
    String geopoliticalZone,
    String state,
    String city,
    String profession,
    String academicQualification,
    String gender,
    LocalDate dateOfBirth) {}
