package com.infopouch.api.modules.uploads.infrastructure;

import com.infopouch.api.modules.research.presentation.dto.PresignedUrlRequest;
import com.infopouch.api.modules.research.presentation.dto.PresignedUrlResponse;
import com.infopouch.api.modules.uploads.application.UploadUseCase;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MockUploadService implements UploadUseCase {

  private static final long MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024; // 15MB
  private static final long VIEW_URL_VALIDITY_SECONDS = 60;

  @Override
  public PresignedUrlResponse generatePresignedUploadUrl(PresignedUrlRequest request) {
    // Validation Rule 1: PDF enforcement
    if (!"application/pdf".equalsIgnoreCase(request.contentType())
        && !request.fileName().endsWith(".pdf")) {
      throw new IllegalArgumentException("Invalid file format. Only PDF files are supported.");
    }

    // Validation Rule 2: Limit files to 15MB
    if (request.fileSizeBytes() > MAX_FILE_SIZE_BYTES) {
      throw new IllegalArgumentException(
          "File size limit exceeded. Maximum allowable size is 15MB.");
    }

    String uniqueFileId = UUID.randomUUID().toString();
    String fileKey = "research/papers/" + uniqueFileId + "_" + request.fileName();

    // Emulated signed URL endpoint target back into our application workspace or local storage
    // context
    String uploadUrl =
        "https://infopouch-storage-mock.s3.eu-west-1.amazonaws.com/"
            + fileKey
            + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=mock_key";
    String downloadUrl = "https://infopouch-storage-mock.s3.eu-west-1.amazonaws.com/" + fileKey;

    return new PresignedUrlResponse(uploadUrl, fileKey, downloadUrl);
  }

  @Override
  public String generateSignedViewUrl(String fileKey) {
    // Minted fresh per authorized request and expires quickly so a leaked link goes dead fast;
    // the caller is responsible for checking access before calling this.
    long expiresAtEpochSeconds = (System.currentTimeMillis() / 1000) + VIEW_URL_VALIDITY_SECONDS;
    return "https://infopouch-storage-mock.s3.eu-west-1.amazonaws.com/"
        + fileKey
        + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=mock_key"
        + "&X-Amz-Expires="
        + VIEW_URL_VALIDITY_SECONDS
        + "&X-Amz-Date="
        + expiresAtEpochSeconds;
  }
}
