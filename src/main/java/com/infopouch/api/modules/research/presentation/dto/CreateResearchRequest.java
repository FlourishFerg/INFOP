package com.infopouch.api.modules.research.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateResearchRequest(
    @NotBlank @Size(max = 255) String title,
    @NotEmpty(message = "At least one author is required") List<String> authors,
    @NotBlank String institution,
    @NotNull @Min(1900) @Max(2100) Integer publicationYear,
    @NotBlank(message = "Research field/discipline is required") String researchField,
    @NotBlank(message = "Country of study is required") String countryOfStudy,
    @NotBlank(message = "Research methodology is required") String methodology,
    @NotBlank(message = "File type is required") String fileType,
    @NotBlank @Size(min = 20) String abstractText,
    @NotBlank String fileKey,
    @NotBlank String fileUrl,
    @NotNull Long fileSizeBytes,
    @NotEmpty @Size(min = 5, message = "Provide at least 5 keywords for semantic cataloging")
        List<String> keywords) {}
