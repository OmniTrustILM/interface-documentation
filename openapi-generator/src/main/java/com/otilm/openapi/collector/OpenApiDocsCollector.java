package com.otilm.openapi.collector;

import com.otilm.openapi.config.loader.GroupsConfigLoader;
import com.otilm.openapi.config.model.GroupConfiguration;
import com.otilm.openapi.config.model.GroupsConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads OpenAPI YAML specs from a running Spring Boot application for all groups defined in groups.yaml and saves
 * them to the output directory.
 * <p>
 * Arguments: args[0] - Path to groups.yaml args[1] - Server port args[2] - Output directory
 */
public class OpenApiDocsCollector {
    private static final Logger log = LoggerFactory.getLogger(OpenApiDocsCollector.class);

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            log.error("Usage: OpenApiDocsCollector <groups.yaml> <server-port> <output-dir>");
            System.exit(1);
        }

        String groupsYamlPath = args[0];
        int serverPort = Integer.parseInt(args[1]);
        Path outputDir = Paths.get(args[2]);

        log.info("Generating OpenAPI documentation files...");
        log.info("Groups config: {}", groupsYamlPath);
        log.info("Server port:   {}", serverPort);
        log.info("Output directory: {}", outputDir);

        GroupsConfig config = new GroupsConfigLoader().loadFromFilesystem(groupsYamlPath);
        if (config == null) {
            log.error("Error: groups.yaml not found at {}", groupsYamlPath);
            System.exit(1);
        }

        Files.createDirectories(outputDir);

        List<String> failed = new ArrayList<>();
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            for (GroupConfiguration group : config.getGroups()) {
                String url = "http://localhost:" + serverPort + "/v3/api-docs.yaml/" + group.getGroupName();
                Path outputFile = outputDir.resolve(group.getId() + ".yaml");

                log.info("Generating {} from {} ...", group.getId(), url);

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Files.writeString(outputFile, response.body());
                    log.info("  ✓ Generated: {}", outputFile);
                } else {
                    log.error("  ✗ Failed to generate {} (HTTP {})", group.getId(), response.statusCode());
                    log.error("    URL: {}", url);
                    failed.add(group.getId());
                }
            }
        }

        if (!failed.isEmpty()) {
            log.error("Failed to generate {} OpenAPI documentation file(s): {}", failed.size(), failed);
            System.exit(1);
        }

        log.info("Successfully generated {} OpenAPI documentation files", config.getGroups().size());
        log.info("OpenAPI documentation generation complete!");
    }
}
