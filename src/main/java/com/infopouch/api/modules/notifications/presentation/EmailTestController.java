package com.infopouch.api.modules.notifications.presentation;

import com.infopouch.api.modules.notifications.application.LocalEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailTestController {

  private final LocalEmailService emailService;

  @PostMapping("/send-test")
  public ResponseEntity<String> sendTestEmail(@RequestParam String recipient) {
    try {
      emailService.sendVerificationEmail(recipient, "test-token-12345");
      return ResponseEntity.ok("Verification email sent to " + recipient);
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
    }
  }

  @PostMapping("/send-reset")
  public ResponseEntity<String> sendResetEmail(@RequestParam String recipient) {
    try {
      emailService.sendPasswordResetEmail(recipient, "reset-token-67890");
      return ResponseEntity.ok("Password reset email sent to " + recipient);
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
    }
  }
}
