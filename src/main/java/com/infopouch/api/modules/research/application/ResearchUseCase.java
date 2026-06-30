package com.infopouch.api.modules.research.application;

import com.infopouch.api.modules.research.domain.ResearchStatus;
import com.infopouch.api.modules.research.presentation.dto.*;
import java.util.List;

public interface ResearchUseCase {
  ResearchResponse createResearch(CreateResearchRequest request, String currentUserEmail);

  List<ResearchPaperResponse> getUserResearch(String currentUserEmail);

  ResearchResponse getResearchById(String id);

  ResearchResponse updateResearch(
      String id, CreateResearchRequest request, String currentUserEmail);

  void deleteResearch(String id, String currentUserEmail);

  List<ResearchPaperResponse> getByStatus(ResearchStatus status);

  ResearchResponse approveResearch(String id);

  ResearchResponse rejectResearch(String id, String reason);

  ResearchResponse getPublicResearchById(String id);

  ResearchResponse getViewableResearch(String id, String currentUserEmail);

  List<ResearchPaperResponse> searchPublicResearch(
      String query, String researchField, String institution, Integer year, String country);

  ShareLinkResponse createShareLink(String id, String currentUserEmail);

  ResearchResponse resolveShareLink(String token);

  String generateCitation(String id, String format);
}
