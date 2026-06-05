package com.infopouch.api.modules.uploads.presentation;

import com.infopouch.api.modules.auth.presentation.dto.ApiResponse;
import com.infopouch.api.modules.research.presentation.dto.PresignedUrlRequest;
import com.infopouch.api.modules.research.presentation.dto.PresignedUrlResponse;
import com.infopouch.api.modules.uploads.application.UploadUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

  private final UploadUseCase uploadUseCase;

  @PostMapping("/presigned-url")
  public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
      @Valid @RequestBody PresignedUrlRequest request) {
    PresignedUrlResponse response = uploadUseCase.generatePresignedUploadUrl(request);
    return ResponseEntity.ok(
        ApiResponse.success("Secure cloud storage entry channel leased successfully.", response));
  }
}
