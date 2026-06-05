package com.infopouch.api.modules.research.application;

import com.infopouch.api.modules.research.domain.*;
import com.infopouch.api.modules.research.infrastructure.JpaResearchRepository;
import com.infopouch.api.modules.research.presentation.dto.*;
import com.infopouch.api.modules.users.domain.User;
import com.infopouch.api.modules.users.infrastructure.JpaUserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResearchUseCaseImpl implements ResearchUseCase {

  private final JpaResearchRepository researchRepository;
  private final JpaUserRepository userRepository;

  @Override
  @Transactional
  public ResearchResponse createResearch(CreateResearchRequest request, String currentUserEmail) {
    User user =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(() -> new IllegalArgumentException("User principal not found."));

    ResearchPaper paper =
        ResearchPaper.builder()
            .user(user)
            .title(request.title())
            .abstractText(request.abstractText())
            .fileKey(request.fileKey())
            .fileUrl(request.fileUrl())
            .fileSizeBytes(request.fileSizeBytes())
            .status(ResearchStatus.PENDING)
            .build();

    List<ResearchKeyword> keywords =
        request.keywords().stream()
            .map(
                kw ->
                    ResearchKeyword.builder()
                        .researchPaper(paper)
                        .keyword(kw.toLowerCase().trim())
                        .build())
            .collect(Collectors.toList());

    paper.setKeywords(keywords);
    ResearchPaper savedPaper = researchRepository.save(paper);
    return mapToResponse(savedPaper);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ResearchPaperResponse> getUserResearch(String currentUserEmail) {
    User user =
        userRepository
            .findByEmail(currentUserEmail)
            .orElseThrow(
                () -> new IllegalArgumentException("User profile resolving match failure."));
    return researchRepository.findByUserId(user.getId()).stream()
        .map(this::mapToPaperResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public ResearchResponse getResearchById(String id) {
    ResearchPaper paper =
        researchRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Research paper requested details context missing: " + id));
    return mapToResponse(paper);
  }

  @Override
  @Transactional
  public ResearchResponse updateResearch(
      String id, CreateResearchRequest request, String currentUserEmail) {
    ResearchPaper paper =
        researchRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Research target entry not located."));

    if (!paper.getUser().getEmail().equals(currentUserEmail)) {
      throw new IllegalStateException("Unauthorized profile change action context violation.");
    }

    // Business rules: Allow edits/re-uploads primarily when REJECTED or still PENDING
    paper.setTitle(request.title());
    paper.setAbstractText(request.abstractText());
    paper.setFileKey(request.fileKey());
    paper.setFileUrl(request.fileUrl());
    paper.setFileSizeBytes(request.fileSizeBytes());
    paper.setStatus(ResearchStatus.PENDING); // Reset cycle evaluation pipeline trigger
    paper.setRejectionReason(null);

    paper.getKeywords().clear();
    List<ResearchKeyword> updatedKeywords =
        request.keywords().stream()
            .map(
                kw ->
                    ResearchKeyword.builder()
                        .researchPaper(paper)
                        .keyword(kw.toLowerCase().trim())
                        .build())
            .collect(Collectors.toList());
    paper.getKeywords().addAll(updatedKeywords);

    return mapToResponse(researchRepository.save(paper));
  }

  @Override
  @Transactional
  public void deleteResearch(String id, String currentUserEmail) {
    ResearchPaper paper =
        researchRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Target artifact deletion match missing."));

    if (!paper.getUser().getEmail().equals(currentUserEmail)) {
      throw new IllegalStateException("Security evaluation failed.");
    }
    researchRepository.delete(paper);
  }

  private ResearchResponse mapToResponse(ResearchPaper paper) {
    return new ResearchResponse(
        paper.getId(),
        paper.getUser().getId(),
        paper.getTitle(),
        paper.getAbstractText(),
        paper.getFileUrl(),
        paper.getStatus(),
        paper.getRejectionReason(),
        paper.getKeywords().stream().map(ResearchKeyword::getKeyword).collect(Collectors.toList()),
        paper.getCreatedAt());
  }

  private ResearchPaperResponse mapToPaperResponse(ResearchPaper paper) {
    return new ResearchPaperResponse(
        paper.getId(), paper.getTitle(), paper.getStatus(), paper.getCreatedAt());
  }
}
