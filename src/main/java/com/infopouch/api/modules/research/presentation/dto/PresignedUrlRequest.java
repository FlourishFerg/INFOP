package com.infopouch.api.modules.research.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUrlRequest(
    @NotBlank String fileName,
    @NotBlank String contentType, // Expected: application/pdf
    @NotNull Long fileSizeBytes) {}
