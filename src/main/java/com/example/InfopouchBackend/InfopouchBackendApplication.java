package com.example.InfopouchBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.example.InfopouchBackend", "com.infopouch.api"})
@EnableJpaRepositories(
    basePackages = {
      "com.infopouch.api.modules.users.infrastructure",
      "com.infopouch.api.modules.auth.infrastructure",
      "com.infopouch.api.modules.research.infrastructure",
      "com.infopouch.api.modules.notifications.infrastructure"
    })
@EntityScan(
    basePackages = {
      "com.infopouch.api.modules.users.domain",
      "com.infopouch.api.modules.auth.domain",
      "com.infopouch.api.modules.research.domain",
      "com.infopouch.api.modules.notifications.domain"
    })
public class InfopouchBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(InfopouchBackendApplication.class, args);
  }
}
