package com.infopouch.api.modules.auth.presentation.dto;

public record JwtResponse(String accessToken, String refreshToken, String userId, String email) {}
