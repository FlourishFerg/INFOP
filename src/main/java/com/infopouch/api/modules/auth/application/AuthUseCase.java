package com.infopouch.api.modules.auth.application;

import com.infopouch.api.modules.auth.presentation.dto.*;

public interface AuthUseCase {
  void register(RegisterRequest request);

  void verifyEmail(String token);

  JwtResponse login(LoginRequest request);

  JwtResponse refresh(TokenRefreshRequest request);

  void initiatePasswordReset(String email);

  void completePasswordReset(ResetPasswordRequest request);

  boolean validatePasswordResetToken(String token);

  void logout(String token);
}
