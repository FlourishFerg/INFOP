package com.infopouch.api.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring Boot 4.0 removed FlywayAutoConfiguration. This bean replicates the missing
 * auto-configuration so Flyway migrations run on startup when spring.flyway.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", matchIfMissing = false)
public class FlywayConfig {

  @Bean
  public Flyway flyway(Environment env) {
    String url = env.getProperty("spring.flyway.url", env.getProperty("spring.datasource.url", ""));
    String user =
        env.getProperty("spring.flyway.user", env.getProperty("spring.datasource.username", ""));
    String password =
        env.getProperty(
            "spring.flyway.password", env.getProperty("spring.datasource.password", ""));
    String locations = env.getProperty("spring.flyway.locations", "classpath:db/migration");
    boolean baselineOnMigrate =
        Boolean.parseBoolean(env.getProperty("spring.flyway.baseline-on-migrate", "false"));
    String baselineVersion = env.getProperty("spring.flyway.baseline-version", "1");

    Flyway flyway =
        Flyway.configure()
            .dataSource(url, user, password)
            .locations(locations)
            .baselineOnMigrate(baselineOnMigrate)
            .baselineVersion(baselineVersion)
            .load();
    flyway.migrate();
    return flyway;
  }
}
