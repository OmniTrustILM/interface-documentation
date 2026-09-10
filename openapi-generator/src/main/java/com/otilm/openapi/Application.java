package com.otilm.openapi;

import java.time.Duration;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = {"com.otilm.openapi.config", "com.otilm.openapi.generated"})
public class Application extends SpringBootServletInitializer {

    static {
        // springdoc 2.9.x renders java.time.Duration as `type: string, format: duration`. Redocly's
        // recommended-strict no-invalid-schema-examples rule then rejects sub-second ISO-8601 examples
        // (e.g. PT0.1S), which the platform's Duration fields legitimately use. Render Duration as a plain
        // string so the ISO value stays (documented in each field's description) without the strict format.
        SpringDocUtils.getConfig().replaceWithClass(Duration.class, String.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }
}
