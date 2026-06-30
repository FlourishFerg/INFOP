package com.infopouch.api.modules.research.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectResearchRequest(@NotBlank String reason) {}
