package com.infopouch.api.modules.users.presentation.dto;

import com.infopouch.api.modules.users.domain.MembershipTier;
import jakarta.validation.constraints.NotNull;

public record SelectMembershipRequest(@NotNull MembershipTier membershipTier) {}
