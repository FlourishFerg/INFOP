package com.infopouch.api.modules.users.application;

import com.infopouch.api.common.exception.ValidationException;
import com.infopouch.api.modules.users.domain.MembershipTier;
import com.infopouch.api.modules.users.domain.Profile;
import com.infopouch.api.modules.users.domain.ProfileType;
import com.infopouch.api.modules.users.domain.User;
import com.infopouch.api.modules.users.infrastructure.JpaProfileRepository;
import com.infopouch.api.modules.users.presentation.dto.CompleteProfileRequest;
import com.infopouch.api.modules.users.presentation.dto.ProfileResponse;
import com.infopouch.api.modules.users.presentation.dto.SelectMembershipRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileUseCaseImpl implements ProfileUseCase {

  private final JpaProfileRepository profileRepository;

  @Override
  @Transactional(readOnly = true)
  public ProfileResponse getMyProfile(String currentUserEmail) {
    Profile profile = findProfileByEmail(currentUserEmail);
    return mapToResponse(profile);
  }

  @Override
  @Transactional
  public ProfileResponse selectMembership(
      SelectMembershipRequest request, String currentUserEmail) {
    Profile profile = findProfileByEmail(currentUserEmail);

    if (profile.getProfileType() == ProfileType.GUEST
        && request.membershipTier() == MembershipTier.PREMIUM) {
      throw new ValidationException("Guest accounts cannot be premium.");
    }

    profile.setMembershipTier(request.membershipTier());
    return mapToResponse(profileRepository.save(profile));
  }

  @Override
  @Transactional
  public ProfileResponse completeProfile(CompleteProfileRequest request, String currentUserEmail) {
    Profile profile = findProfileByEmail(currentUserEmail);

    profile.setPhoneNumber(request.phoneNumber());
    profile.setCountry(request.country());
    profile.setGeopoliticalZone(request.geopoliticalZone());
    profile.setState(request.state());
    profile.setCity(request.city());
    profile.setProfession(request.profession());
    profile.setAcademicQualification(request.academicQualification());
    profile.setGender(request.gender());
    profile.setDateOfBirth(request.dateOfBirth());
    profile.setOnboardingCompleted(true);

    return mapToResponse(profileRepository.save(profile));
  }

  private Profile findProfileByEmail(String email) {
    return profileRepository
        .findByUserEmail(email)
        .orElseThrow(() -> new IllegalStateException("Profile not found for user."));
  }

  private ProfileResponse mapToResponse(Profile profile) {
    User user = profile.getUser();
    return new ProfileResponse(
        profile.getId(),
        user.getId(),
        user.getEmail(),
        profile.getFullName(),
        user.getRole(),
        profile.getProfileType(),
        profile.getMembershipTier(),
        profile.getPhoneNumber(),
        profile.getCountry(),
        profile.getGeopoliticalZone(),
        profile.getState(),
        profile.getCity(),
        profile.getProfession(),
        profile.getAcademicQualification(),
        profile.getGender(),
        profile.getDateOfBirth(),
        user.isVerified(),
        profile.isOnboardingCompleted(),
        profile.getCreatedAt());
  }
}
