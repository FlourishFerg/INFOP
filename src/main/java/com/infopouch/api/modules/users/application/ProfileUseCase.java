package com.infopouch.api.modules.users.application;

import com.infopouch.api.modules.users.presentation.dto.CompleteProfileRequest;
import com.infopouch.api.modules.users.presentation.dto.ProfileResponse;
import com.infopouch.api.modules.users.presentation.dto.SelectMembershipRequest;

public interface ProfileUseCase {
  ProfileResponse getMyProfile(String currentUserEmail);

  ProfileResponse selectMembership(SelectMembershipRequest request, String currentUserEmail);

  ProfileResponse completeProfile(CompleteProfileRequest request, String currentUserEmail);
}
