package com.infopouch.api.modules.research.presentation.dto;

import java.time.LocalDateTime;

public record ShareLinkResponse(String shareUrl, String token, LocalDateTime expiresAt) {}
