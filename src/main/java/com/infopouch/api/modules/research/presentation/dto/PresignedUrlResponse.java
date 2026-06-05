package com.infopouch.api.modules.research.presentation.dto;

public record PresignedUrlResponse(String uploadUrl, String fileKey, String downloadUrl) {}
