package com.infopouch.api.modules.auth.application;

import com.infopouch.api.modules.auth.domain.AuthToken;
import com.infopouch.api.modules.auth.presentation.dto.*;
import com.infopouch.api.modules.notifications.application.LocalEmailService;
import com.infopouch.api.modules.users.domain.Profile;
import com.infopouch.api.modules.users.domain.Role;
import com.infopouch.api.modules.users.domain.User;
import com.infopouch.api.modules.users.infrastructure.JpaAuthTokenRepository;
import com.infopouch.api.modules.users.infrastructure.JpaProfileRepository;
import com.infopouch.api.modules.users.infrastructure.JpaUserRepository;
import com.infopouch.api.security.JwtService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthUseCaseImpl implements AuthUseCase {

  private final JpaUserRepository userRepository;
  private final JpaProfileRepository profileRepository;
  private final JpaAuthTokenRepository authTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final LocalEmailService emailService;

  @Override
  @Transactional
  public void register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("Email address is already registered.");
    }

    Role role =
        switch (request.profileType()) {
          case STUDENT -> Role.STUDENT;
          case LECTURER -> Role.LECTURER;
          case PROFESSIONAL -> Role.PROFESSIONAL;
          case GUEST -> Role.GUEST;
        };

    // 1. Create and persist User base
    User user =
        User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(role)
            .isVerified(false)
            .build();

    User savedUser = userRepository.save(user);

    // 2. Create the profile shell - membership tier and remaining details are
    // collected in the Membership Selection and Profile Completion steps
    Profile profile =
        Profile.builder()
            .user(savedUser)
            .fullName(request.fullName())
            .profileType(request.profileType())
            .build();

    profileRepository.save(profile);

    // 3. Handle Token Generation & Trigger verification mock link
    String tokenValue = UUID.randomUUID().toString();
    AuthToken verificationToken =
        AuthToken.builder()
            .tokenValue(tokenValue)
            .tokenType("VERIFICATION")
            .user(savedUser)
            .expiryDate(LocalDateTime.now().plusHours(24))
            .isRevoked(false)
            .build();

    authTokenRepository.save(verificationToken);
    emailService.sendVerificationEmail(savedUser.getEmail(), tokenValue);
  }

  @Override
  @Transactional
  public void verifyEmail(String token) {
    AuthToken authToken =
        authTokenRepository
            .findByTokenValue(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid verification token."));

    if (!"VERIFICATION".equals(authToken.getTokenType()) || authToken.isRevoked()) {
      throw new IllegalArgumentException("Invalid token context.");
    }

    if (authToken.getExpiryDate().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Verification token has expired.");
    }

    User user = authToken.getUser();
    user.setVerified(true);
    userRepository.save(user);

    authTokenRepository.delete(authToken);
  }

  @Override
  @Transactional
  public JwtResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(
                () -> new IllegalArgumentException("Invalid email or password credentials."));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid email or password credentials.");
    }

    if (!user.isVerified()) {
      throw new IllegalStateException(
          "Please verify your email address before attempting to log in.");
    }

    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    // Save refresh token reference to database to keep track of active sessions
    AuthToken sessionToken =
        AuthToken.builder()
            .tokenValue(refreshToken)
            .tokenType("REFRESH")
            .user(user)
            .expiryDate(LocalDateTime.now().plusDays(7))
            .isRevoked(false)
            .build();

    authTokenRepository.save(sessionToken);

    Profile profile =
        profileRepository
            .findByUserId(user.getId())
            .orElseThrow(() -> new IllegalStateException("Profile not found for user."));

    return new JwtResponse(
        accessToken,
        refreshToken,
        user.getId(),
        user.getEmail(),
        profile.getMembershipTier(),
        profile.isOnboardingCompleted());
  }

  @Override
  @Transactional
  public JwtResponse refresh(TokenRefreshRequest request) {
    String tokenValue = request.refreshToken();

    AuthToken savedToken =
        authTokenRepository
            .findByTokenValue(tokenValue)
            .orElseThrow(() -> new IllegalArgumentException("Refresh token not found or revoked."));

    if (savedToken.isRevoked() || savedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Refresh token has expired or been revoked.");
    }

    if (!"REFRESH".equals(savedToken.getTokenType())) {
      throw new IllegalArgumentException("Provided token is not a valid refresh token.");
    }

    User user = savedToken.getUser();
    String newAccessToken = jwtService.generateAccessToken(user);

    Profile profile =
        profileRepository
            .findByUserId(user.getId())
            .orElseThrow(() -> new IllegalStateException("Profile not found for user."));

    return new JwtResponse(
        newAccessToken,
        tokenValue,
        user.getId(),
        user.getEmail(),
        profile.getMembershipTier(),
        profile.isOnboardingCompleted());
  }

  @Override
  @Transactional
  public void initiatePasswordReset(String email) {
    // Quietly return on missing users to prevent account enumeration vectors
    userRepository
        .findByEmail(email)
        .ifPresent(
            user -> {
              String tokenValue = UUID.randomUUID().toString();

              AuthToken resetToken =
                  AuthToken.builder()
                      .tokenValue(tokenValue)
                      .tokenType("PASSWORD_RESET")
                      .user(user)
                      .expiryDate(LocalDateTime.now().plusMinutes(15)) // 15 minutes window
                      .isRevoked(false)
                      .build();

              authTokenRepository.save(resetToken);
              try {
                emailService.sendPasswordResetEmail(user.getEmail(), tokenValue);
              } catch (Exception ex) {
                // Swallow send failures here: surfacing them would let callers infer
                // whether the email exists based on success vs. failure responses.
                log.error("Failed to send password reset email to {}", user.getEmail(), ex);
              }
            });
  }

  @Override
  @Transactional
  public void completePasswordReset(ResetPasswordRequest request) {
    AuthToken tokenRecord =
        authTokenRepository
            .findByTokenValue(request.token())
            .orElseThrow(
                () -> new IllegalArgumentException("Invalid or expired password reset link."));

    if (!"PASSWORD_RESET".equals(tokenRecord.getTokenType()) || tokenRecord.isRevoked()) {
      throw new IllegalArgumentException("Invalid token context.");
    }

    if (tokenRecord.getExpiryDate().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Password reset link has expired.");
    }

    User user = tokenRecord.getUser();
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    authTokenRepository.delete(tokenRecord);
  }

  @Override
  @Transactional
  public void logout(String token) {
    // If it's a refresh token string, remove it directly to sever session authority
    authTokenRepository.findByTokenValue(token).ifPresent(authTokenRepository::delete);

    // Note: Access tokens are stateless, so they are cleanly discarded by client systems.
    // For absolute security in week 3, blacklisted access tokens can be added to Redis.
  }

  @Override
  @Transactional(readOnly = true)
  public boolean validatePasswordResetToken(String token) {
    return authTokenRepository
        .findByTokenValue(token)
        .filter(t -> "PASSWORD_RESET".equals(t.getTokenType()))
        .filter(t -> !t.isRevoked())
        .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
        .isPresent();
  }
}
