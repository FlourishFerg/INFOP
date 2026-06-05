package com.infopouch.api.modules.research.presentation.dto;

import com.infopouch.api.modules.research.domain.ResearchStatus;
import java.time.LocalDateTime;

public record ResearchPaperResponse(
    String id, String title, ResearchStatus status, LocalDateTime createdAt) {}
