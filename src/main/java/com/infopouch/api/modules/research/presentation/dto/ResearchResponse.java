package com.infopouch.api.modules.research.presentation.dto;

import com.infopouch.api.modules.research.domain.ResearchStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ResearchResponse(
    String id,
    String userId,
    String title,
    List<String> authors,
    String institution,
    Integer publicationYear,
    String researchField,
    String countryOfStudy,
    String methodology,
    String fileType,
    String abstractText,
    String fileUrl,
    ResearchStatus status,
    String rejectionReason,
    List<String> keywords,
    LocalDateTime createdAt) {}
