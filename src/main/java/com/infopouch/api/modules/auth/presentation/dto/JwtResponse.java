package com.infopouch.api.modules.auth.presentation.dto;

import com.infopouch.api.modules.users.domain.MembershipTier;

public record JwtResponse(
    String accessToken,
    String refreshToken,
    String userId,
    String email,
    MembershipTier membershipTier,
    boolean onboardingCompleted) {}
