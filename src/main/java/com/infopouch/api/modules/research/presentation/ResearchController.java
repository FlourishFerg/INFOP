package com.infopouch.api.modules.research.presentation;

import com.infopouch.api.modules.auth.presentation.dto.ApiResponse;
import com.infopouch.api.modules.research.application.ResearchUseCase;
import com.infopouch.api.modules.research.presentation.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/research")
@RequiredArgsConstructor
public class ResearchController {

  private final ResearchUseCase researchUseCase;

  @PostMapping
  public ResponseEntity<ApiResponse<ResearchResponse>> createResearch(
      @Valid @RequestBody CreateResearchRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    ResearchResponse response = researchUseCase.createResearch(request, userDetails.getUsername());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                "Research upload registered into pending verification pipeline successfully.",
                response));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<ResearchPaperResponse>>> getUserResearch(
      @AuthenticationPrincipal UserDetails userDetails) {
    List<ResearchPaperResponse> data = researchUseCase.getUserResearch(userDetails.getUsername());
    return ResponseEntity.ok(
        ApiResponse.success("User research documents list loaded successfully.", data));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ResearchResponse>> getResearchById(@PathVariable String id) {
    ResearchResponse response = researchUseCase.getResearchById(id);
    return ResponseEntity.ok(ApiResponse.success("Research profile trace completed.", response));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ResearchResponse>> updateResearch(
      @PathVariable String id,
      @Valid @RequestBody CreateResearchRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    ResearchResponse response =
        researchUseCase.updateResearch(id, request, userDetails.getUsername());
    return ResponseEntity.ok(
        ApiResponse.success("Research data version revision updated cleanly.", response));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<String>> deleteResearch(
      @PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
    researchUseCase.deleteResearch(id, userDetails.getUsername());
    return ResponseEntity.ok(ApiResponse.success("Research entry purged successfully."));
  }
}
