package com.infopouch.api.modules.research.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateResearchRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(min = 20) String abstractText,
    @NotBlank String fileKey,
    @NotBlank String fileUrl,
    @NotNull java.lang.Long fileSizeBytes,
    @NotEmpty @Size(min = 3, message = "Provide at least 3 keywords for semantic cataloging")
        List<String> keywords) {}
