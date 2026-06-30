package com.infopouch.api.modules.research.application;

import com.infopouch.api.common.exception.ResourceNotFoundException;
import com.infopouch.api.modules.notifications.application.LocalEmailService;
import com.infopouch.api.modules.notifications.application.NotificationUseCase;
import com.infopouch.api.modules.notifications.domain.NotificationType;
import com.infopouch.api.modules.research.domain.*;
import com.infopouch.api.modules.research.infrastructure.JpaResearchRepository;
import com.infopouch.api.modules.research.infrastructure.JpaResearchShareTokenRepository;
import com.infopouch.api.modules.research.infrastructure.ResearchSpecifications;
import com.infopouch.api.modules.research.presentation.dto.*;
import com.infopouch.api.modules.uploads.application.UploadUseCase;
import com.infopouch.api.modules.users.domain.User;
import com.infopouch.api.modules.users.infrastructure.JpaUserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResearchUseCaseImpl implements ResearchUseCase {

  private static final String AUTHOR_DELIMITER = "; ";
  private static final int SHARE_LINK_VALIDITY_DAYS = 7;

  private final JpaResearchRepository researchRepository;
  private final JpaResearchShareTokenRepository shareTokenRepository;
  private final JpaUserRepository userRepository;
  private final LocalEmailService emailService;
  private final NotificationUseCase notificationUseCase;
  private final UploadUseCase uploadUseCase;

  @Value("${app.frontend-url:${app.base-url}}")
  private String frontendUrl;

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
            .authors(String.join(AUTHOR_DELIMITER, request.authors()))
            .institution(request.institution())
            .publicationYear(request.publicationYear())
            .researchField(request.researchField())
            .countryOfStudy(request.countryOfStudy())
            .methodology(request.methodology())
            .fileType(request.fileType())
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
    return mapToResponse(findByIdOrThrow(id));
  }

  @Override
  @Transactional
  public ResearchResponse updateResearch(
      String id, CreateResearchRequest request, String currentUserEmail) {
    ResearchPaper paper = findByIdOrThrow(id);

    if (!paper.getUser().getEmail().equals(currentUserEmail)) {
      throw new IllegalStateException("Unauthorized profile change action context violation.");
    }

    if (paper.getStatus() == ResearchStatus.APPROVED) {
      throw new IllegalStateException("An approved research paper can no longer be edited.");
    }

    paper.setTitle(request.title());
    paper.setAuthors(String.join(AUTHOR_DELIMITER, request.authors()));
    paper.setInstitution(request.institution());
    paper.setPublicationYear(request.publicationYear());
    paper.setResearchField(request.researchField());
    paper.setCountryOfStudy(request.countryOfStudy());
    paper.setMethodology(request.methodology());
    paper.setFileType(request.fileType());
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
    ResearchPaper paper = findByIdOrThrow(id);

    if (!paper.getUser().getEmail().equals(currentUserEmail)) {
      throw new IllegalStateException("Security evaluation failed.");
    }
    researchRepository.delete(paper);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ResearchPaperResponse> getByStatus(ResearchStatus status) {
    return researchRepository.findByStatus(status).stream()
        .map(this::mapToPaperResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public ResearchResponse approveResearch(String id) {
    ResearchPaper paper = findByIdOrThrow(id);
    paper.setStatus(ResearchStatus.APPROVED);
    paper.setRejectionReason(null);
    ResearchPaper saved = researchRepository.save(paper);

    notificationUseCase.createNotification(
        saved.getUser(),
        NotificationType.RESEARCH_APPROVED,
        "Research approved",
        "Your research paper \"" + saved.getTitle() + "\" has been approved.");
    emailService.sendResearchApprovedEmail(saved.getUser().getEmail(), saved.getTitle());

    return mapToResponse(saved);
  }

  @Override
  @Transactional
  public ResearchResponse rejectResearch(String id, String reason) {
    ResearchPaper paper = findByIdOrThrow(id);
    paper.setStatus(ResearchStatus.REJECTED);
    paper.setRejectionReason(reason);
    ResearchPaper saved = researchRepository.save(paper);

    notificationUseCase.createNotification(
        saved.getUser(),
        NotificationType.RESEARCH_REJECTED,
        "Research rejected",
        "Your research paper \"" + saved.getTitle() + "\" was rejected: " + reason);
    emailService.sendResearchRejectedEmail(saved.getUser().getEmail(), saved.getTitle(), reason);

    return mapToResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public ResearchResponse getPublicResearchById(String id) {
    ResearchPaper paper = findByIdOrThrow(id);
    if (paper.getStatus() != ResearchStatus.APPROVED) {
      throw new ResourceNotFoundException("Research paper not found: " + id);
    }
    return mapToResponse(paper);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ResearchPaperResponse> searchPublicResearch(
      String query, String researchField, String institution, Integer year, String country) {
    return researchRepository
        .findAll(
            ResearchSpecifications.search(
                ResearchStatus.APPROVED, query, researchField, institution, year, country))
        .stream()
        .map(this::mapToPaperResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public ResearchResponse getViewableResearch(String id, String currentUserEmail) {
    ResearchPaper paper = findByIdOrThrow(id);
    boolean isOwner = paper.getUser().getEmail().equals(currentUserEmail);
    if (!isOwner && paper.getStatus() != ResearchStatus.APPROVED) {
      throw new ResourceNotFoundException("Research paper not found: " + id);
    }
    return mapToResponse(paper);
  }

  @Override
  @Transactional
  public ShareLinkResponse createShareLink(String id, String currentUserEmail) {
    ResearchPaper paper = findByIdOrThrow(id);

    boolean isOwner = paper.getUser().getEmail().equals(currentUserEmail);
    if (!isOwner && paper.getStatus() != ResearchStatus.APPROVED) {
      throw new ResourceNotFoundException("Research paper not found: " + id);
    }

    ResearchShareToken shareToken =
        ResearchShareToken.builder()
            .researchPaper(paper)
            .expiresAt(LocalDateTime.now().plusDays(SHARE_LINK_VALIDITY_DAYS))
            .build();
    ResearchShareToken saved = shareTokenRepository.save(shareToken);

    return new ShareLinkResponse(
        frontendUrl + "/research/shared/" + saved.getToken(),
        saved.getToken(),
        saved.getExpiresAt());
  }

  @Override
  @Transactional(readOnly = true)
  public ResearchResponse resolveShareLink(String token) {
    ResearchShareToken shareToken =
        shareTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired share link."));

    if (shareToken.isExpired()) {
      throw new ResourceNotFoundException("Invalid or expired share link.");
    }

    return mapToResponse(shareToken.getResearchPaper());
  }

  @Override
  @Transactional(readOnly = true)
  public String generateCitation(String id, String format) {
    ResearchResponse paper = getPublicResearchById(id);
    return CitationFormatter.format(paper, format);
  }

  private ResearchPaper findByIdOrThrow(String id) {
    return researchRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Research paper not found: " + id));
  }

  private ResearchResponse mapToResponse(ResearchPaper paper) {
    return new ResearchResponse(
        paper.getId(),
        paper.getUser().getId(),
        paper.getTitle(),
        splitAuthors(paper.getAuthors()),
        paper.getInstitution(),
        paper.getPublicationYear(),
        paper.getResearchField(),
        paper.getCountryOfStudy(),
        paper.getMethodology(),
        paper.getFileType(),
        paper.getAbstractText(),
        uploadUseCase.generateSignedViewUrl(paper.getFileKey()),
        paper.getStatus(),
        paper.getRejectionReason(),
        paper.getKeywords().stream().map(ResearchKeyword::getKeyword).collect(Collectors.toList()),
        paper.getCreatedAt());
  }

  private ResearchPaperResponse mapToPaperResponse(ResearchPaper paper) {
    return new ResearchPaperResponse(
        paper.getId(),
        paper.getTitle(),
        paper.getInstitution(),
        paper.getPublicationYear(),
        paper.getResearchField(),
        paper.getStatus(),
        paper.getCreatedAt());
  }

  private static List<String> splitAuthors(String authors) {
    return Arrays.stream(authors.split(AUTHOR_DELIMITER)).collect(Collectors.toList());
  }
}
