package com.infopouch.api.modules.research.application;

import com.infopouch.api.modules.research.presentation.dto.*;
import java.util.List;

public interface ResearchUseCase {
  ResearchResponse createResearch(CreateResearchRequest request, String currentUserEmail);

  List<ResearchPaperResponse> getUserResearch(String currentUserEmail);

  ResearchResponse getResearchById(String id);

  ResearchResponse updateResearch(
      String id, CreateResearchRequest request, String currentUserEmail);

  void deleteResearch(String id, String currentUserEmail);
}
