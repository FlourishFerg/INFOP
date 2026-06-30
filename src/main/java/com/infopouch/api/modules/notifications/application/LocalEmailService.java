package com.infopouch.api.modules.notifications.application;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sends email via Resend's HTTP API rather than raw SMTP. Several hosting platforms (e.g. Railway's
 * free tier) block outbound SMTP ports to prevent spam abuse, which an HTTPS-based API call isn't
 * subject to.
 */
@Service
@Slf4j
public class LocalEmailService {

  // Built directly rather than via an injected RestClient.Builder - Spring Boot 4.0
  // doesn't reliably auto-configure that bean (similar to the FlywayAutoConfiguration
  // removal elsewhere in this app), and this avoids depending on it.
  private final RestClient restClient = RestClient.create();

  @Value("${app.base-url}")
  private String baseUrl;

  @Value("${app.frontend-url:${app.base-url}}")
  private String frontendUrl;

  @Value("${resend.api-key}")
  private String apiKey;

  @Value("${resend.from-address:onboarding@resend.dev}")
  private String fromAddress;

  public void sendVerificationEmail(String email, String token) {
    String verificationLink = frontendUrl + "/verify-email?token=" + token;
    String subject = "Verify your InfoPouch account";
    String body =
        "<h2>Welcome to InfoPouch!</h2>"
            + "<p>Thank you for registering.</p>"
            + "<p>Please verify your email by clicking the link below:</p>"
            + "<p><a href=\""
            + verificationLink
            + "\">Verify Email</a></p>"
            + "<p>If you did not sign up, please ignore this message.</p>";
    sendEmail(email, subject, body);
  }

  public void sendPasswordResetEmail(String email, String token) {
    String resetLink = frontendUrl + "/reset-password?token=" + token;
    String subject = "InfoPouch password reset";
    String body =
        "<h2>Password Reset Request</h2>"
            + "<p>We received a request to reset your password.</p>"
            + "<p>Click the link below to choose a new password:</p>"
            + "<p><a href=\""
            + resetLink
            + "\">Reset Password</a></p>"
            + "<p>If you did not request this, no action is required.</p>";
    sendEmail(email, subject, body);
  }

  public void sendResearchApprovedEmail(String email, String researchTitle) {
    String subject = "Your research submission has been approved";
    String body =
        "<h2>Submission Approved</h2>"
            + "<p>Your research paper \""
            + researchTitle
            + "\" has been approved and is now published.</p>";
    sendEmail(email, subject, body);
  }

  public void sendResearchRejectedEmail(String email, String researchTitle, String reason) {
    String subject = "Your research submission requires changes";
    String body =
        "<h2>Submission Rejected</h2>"
            + "<p>Your research paper \""
            + researchTitle
            + "\" was rejected for the following reason:</p>"
            + "<p>"
            + reason
            + "</p>"
            + "<p>You may edit and resubmit it from your dashboard.</p>";
    sendEmail(email, subject, body);
  }

  private void sendEmail(String recipient, String subject, String htmlBody) {
    try {
      restClient
          .post()
          .uri("https://api.resend.com/emails")
          .header("Authorization", "Bearer " + apiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              Map.of(
                  "from", fromAddress,
                  "to", List.of(recipient),
                  "subject", subject,
                  "html", htmlBody))
          .retrieve()
          .toBodilessEntity();
      log.info("Email sent successfully to: {}", recipient);
    } catch (RestClientException exception) {
      log.error("Failed to send email to {}", recipient, exception);
      throw new IllegalStateException("Unable to send email", exception);
    }
  }
}
