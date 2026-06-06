package com.infopouch.api.modules.notifications.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalEmailService {

  private final JavaMailSender mailSender;

  @Value("${app.base-url}")
  private String baseUrl;

  @Value("${app.frontend-url:${app.base-url}}")
  private String frontendUrl;

  @Value("${app.mail.from:demo@infopouch.com}")
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

  private void sendEmail(String recipient, String subject, String htmlBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      mailSender.send(message);
      log.info("Email sent successfully to: {}", recipient);
    } catch (MessagingException exception) {
      log.error("Failed to send email to {}", recipient, exception);
      throw new IllegalStateException("Unable to send email", exception);
    }
  }
}
