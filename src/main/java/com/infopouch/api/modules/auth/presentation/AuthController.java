package com.infopouch.api.modules.auth.presentation;

import com.infopouch.api.modules.auth.application.AuthUseCase;
import com.infopouch.api.modules.auth.presentation.dto.ApiResponse;
import com.infopouch.api.modules.auth.presentation.dto.ForgotPasswordRequest;
import com.infopouch.api.modules.auth.presentation.dto.JwtResponse;
import com.infopouch.api.modules.auth.presentation.dto.LoginRequest;
import com.infopouch.api.modules.auth.presentation.dto.RegisterRequest;
import com.infopouch.api.modules.auth.presentation.dto.ResetPasswordRequest;
import com.infopouch.api.modules.auth.presentation.dto.TokenRefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthUseCase authUseCase;

  /** User Registration */
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {

    authUseCase.register(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                "Registration successful. Please check your email for verification."));
  }

  /** Email Verification */
  @GetMapping("/verify")
  public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {

    authUseCase.verifyEmail(token);

    return ResponseEntity.ok(
        ApiResponse.success("Email verified successfully. You can now log in."));
  }

  /** Login */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {

    JwtResponse response = authUseCase.login(request);

    return ResponseEntity.ok(ApiResponse.success("Login successful", response));
  }

  /** Refresh Access Token */
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(
      @Valid @RequestBody TokenRefreshRequest request) {

    JwtResponse response = authUseCase.refresh(request);

    return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
  }

  /** Forgot Password */
  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<String>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {

    authUseCase.initiatePasswordReset(request.email());

    return ResponseEntity.ok(
        ApiResponse.success("If the email exists, a reset link has been sent."));
  }

  /** Reset Password */
  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<String>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {

    authUseCase.completePasswordReset(request);

    return ResponseEntity.ok(
        ApiResponse.success(
            "Password reset successful. You can now log in with your new password."));
  }

  /** Validate password reset token (for frontend) */
  @GetMapping("/reset/validate")
  public ResponseEntity<ApiResponse<Boolean>> validateResetToken(@RequestParam String token) {

    boolean valid = authUseCase.validatePasswordResetToken(token);

    return ResponseEntity.ok(ApiResponse.success("Token validation result", valid));
  }

  /** Logout */
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<String>> logout(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {

      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ApiResponse.error("Invalid authorization header"));
    }

    String token = authHeader.substring(7);

    authUseCase.logout(token);

    return ResponseEntity.ok(ApiResponse.success("Logged out successfully."));
  }
}
