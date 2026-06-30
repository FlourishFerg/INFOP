package com.infopouch.api.modules.research.presentation;

import com.infopouch.api.modules.auth.presentation.dto.ApiResponse;
import com.infopouch.api.modules.research.application.ResearchUseCase;
import com.infopouch.api.modules.research.domain.ResearchStatus;
import com.infopouch.api.modules.research.presentation.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/research")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminResearchController {

  private final ResearchUseCase researchUseCase;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ResearchPaperResponse>>> getByStatus(
      @RequestParam(defaultValue = "PENDING") ResearchStatus status) {
    List<ResearchPaperResponse> data = researchUseCase.getByStatus(status);
    return ResponseEntity.ok(ApiResponse.success("Research review queue loaded.", data));
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<ApiResponse<ResearchResponse>> approve(@PathVariable String id) {
    ResearchResponse response = researchUseCase.approveResearch(id);
    return ResponseEntity.ok(ApiResponse.success("Research approved successfully.", response));
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<ApiResponse<ResearchResponse>> reject(
      @PathVariable String id, @Valid @RequestBody RejectResearchRequest request) {
    ResearchResponse response = researchUseCase.rejectResearch(id, request.reason());
    return ResponseEntity.ok(ApiResponse.success("Research rejected successfully.", response));
  }
}
