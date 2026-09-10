package com.otilm.openapi.e2e;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application used exclusively by the e2e integration tests. Scans the same packages as the
 * production Application but lives in test scope, so it is never packaged into a deployable artifact.
 */
@SpringBootApplication(scanBasePackages = {"com.otilm.openapi.config", "com.otilm.openapi.generated"})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
