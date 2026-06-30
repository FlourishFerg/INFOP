package com.infopouch.api.modules.research.presentation;

import com.infopouch.api.modules.auth.presentation.dto.ApiResponse;
import com.infopouch.api.modules.research.application.ResearchUseCase;
import com.infopouch.api.modules.research.presentation.dto.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/research")
@RequiredArgsConstructor
public class PublicResearchController {

  private final ResearchUseCase researchUseCase;

  @GetMapping("/public/search")
  public ResponseEntity<ApiResponse<List<ResearchPaperResponse>>> search(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String researchField,
      @RequestParam(required = false) String institution,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) String country) {
    List<ResearchPaperResponse> results =
        researchUseCase.searchPublicResearch(query, researchField, institution, year, country);
    return ResponseEntity.ok(ApiResponse.success("Research search results loaded.", results));
  }

  @GetMapping("/public/{id}")
  public ResponseEntity<ApiResponse<ResearchResponse>> viewPublic(@PathVariable String id) {
    ResearchResponse response = researchUseCase.getPublicResearchById(id);
    return ResponseEntity.ok(ApiResponse.success("Research details loaded.", response));
  }

  @GetMapping("/public/{id}/citation")
  public ResponseEntity<ApiResponse<String>> citation(
      @PathVariable String id, @RequestParam(defaultValue = "APA") String format) {
    String citation = researchUseCase.generateCitation(id, format);
    return ResponseEntity.ok(ApiResponse.success("Citation generated.", citation));
  }

  @GetMapping("/shared/{token}")
  public ResponseEntity<ApiResponse<ResearchResponse>> viewShared(@PathVariable String token) {
    ResearchResponse response = researchUseCase.resolveShareLink(token);
    return ResponseEntity.ok(ApiResponse.success("Shared research loaded.", response));
  }
}
