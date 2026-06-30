package com.infopouch.api.modules.uploads.application;

import com.infopouch.api.modules.research.presentation.dto.PresignedUrlRequest;
import com.infopouch.api.modules.research.presentation.dto.PresignedUrlResponse;

public interface UploadUseCase {
  PresignedUrlResponse generatePresignedUploadUrl(PresignedUrlRequest request);

  String generateSignedViewUrl(String fileKey);
}
