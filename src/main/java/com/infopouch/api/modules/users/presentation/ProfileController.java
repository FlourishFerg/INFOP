package com.infopouch.api.modules.users.presentation;

import com.infopouch.api.modules.auth.presentation.dto.ApiResponse;
import com.infopouch.api.modules.users.application.ProfileUseCase;
import com.infopouch.api.modules.users.presentation.dto.CompleteProfileRequest;
import com.infopouch.api.modules.users.presentation.dto.ProfileResponse;
import com.infopouch.api.modules.users.presentation.dto.SelectMembershipRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

  private final ProfileUseCase profileUseCase;

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
      @AuthenticationPrincipal UserDetails userDetails) {
    ProfileResponse response = profileUseCase.getMyProfile(userDetails.getUsername());
    return ResponseEntity.ok(ApiResponse.success("Profile loaded successfully.", response));
  }

  @PutMapping("/membership")
  public ResponseEntity<ApiResponse<ProfileResponse>> selectMembership(
      @Valid @RequestBody SelectMembershipRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    ProfileResponse response = profileUseCase.selectMembership(request, userDetails.getUsername());
    return ResponseEntity.ok(
        ApiResponse.success("Membership tier selected successfully.", response));
  }

  @PutMapping("/complete")
  public ResponseEntity<ApiResponse<ProfileResponse>> completeProfile(
      @Valid @RequestBody CompleteProfileRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    ProfileResponse response = profileUseCase.completeProfile(request, userDetails.getUsername());
    return ResponseEntity.ok(ApiResponse.success("Profile completed successfully.", response));
  }
}
