package com.otilm.openapi.e2e;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test for the OpenAPI generation pipeline.
 *
 * <p>
 * Spins up a full Spring Boot context (via {@code @SpringBootTest}) using the test interfaces and groups.yaml defined
 * in this module. Then queries the running springdoc endpoint and asserts on the content of the generated YAML
 * documents.
 *
 * <p>
 * No dependency on {@code com.otilm:interfaces} is required — the pipeline is exercised entirely with the
 * self-contained test interfaces in this module.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndToEndIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // Widgets group (TestWidgetController → TestAuthProtectedController)
    // -------------------------------------------------------------------------

    @Test
    void widgetsGroup_returnsHttp200() {
        ResponseEntity<String> response = fetchGroupYaml("e2e-widgets");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP 200 for e2e-widgets group");
    }

    @Test
    void widgetsGroup_containsGetWidgetPath() {
        String yaml = fetchGroupYamlBody("e2e-widgets");
        assertTrue(yaml.contains("/v1/test/widgets/{id}"),
                "Expected path /v1/test/widgets/{id} in e2e-widgets OpenAPI");
    }

    @Test
    void widgetsGroup_containsPostWidgetsPath() {
        String yaml = fetchGroupYamlBody("e2e-widgets");
        assertTrue(yaml.contains("/v1/test/widgets"), "Expected path /v1/test/widgets in e2e-widgets OpenAPI");
    }

    @Test
    void widgetsGroup_containsGetDeleteAndPostOperations() {
        Map<String, Object> doc = parseYaml(fetchGroupYamlBody("e2e-widgets"));
        Map<?, ?> paths = (Map<?, ?>) doc.get("paths");
        assertNotNull(paths, "paths section must be present");

        Map<?, ?> widgetByIdOps = (Map<?, ?>) paths.get("/v1/test/widgets/{id}");
        assertNotNull(widgetByIdOps, "path /v1/test/widgets/{id} must exist");
        assertTrue(widgetByIdOps.containsKey("get"), "GET /v1/test/widgets/{id} must be present");
        assertTrue(widgetByIdOps.containsKey("delete"), "DELETE /v1/test/widgets/{id} must be present");

        Map<?, ?> widgetsOps = (Map<?, ?>) paths.get("/v1/test/widgets");
        assertNotNull(widgetsOps, "path /v1/test/widgets must exist");
        assertTrue(widgetsOps.containsKey("post"), "POST /v1/test/widgets must be present");
    }

    @Test
    void widgetsGroup_containsTestBearerAuthScheme() {
        Map<String, Object> doc = parseYaml(fetchGroupYamlBody("e2e-widgets"));
        Map<?, ?> securitySchemes = extractSecuritySchemes(doc);
        assertTrue(securitySchemes.containsKey("TestBearerAuth"),
                "e2e-widgets group must declare TestBearerAuth security scheme");
    }

    @Test
    void widgetsGroup_containsTestApiKeyAuthScheme() {
        Map<String, Object> doc = parseYaml(fetchGroupYamlBody("e2e-widgets"));
        Map<?, ?> securitySchemes = extractSecuritySchemes(doc);
        assertTrue(securitySchemes.containsKey("TestApiKeyAuth"),
                "e2e-widgets group must declare TestApiKeyAuth security scheme");
    }

    @Test
    void widgetsGroup_operationsReferenceSecuritySchemes() {
        String yaml = fetchGroupYamlBody("e2e-widgets");
        assertTrue(yaml.contains("TestBearerAuth"),
                "Security scheme name TestBearerAuth must appear in e2e-widgets YAML");
        assertTrue(yaml.contains("TestApiKeyAuth"),
                "Security scheme name TestApiKeyAuth must appear in e2e-widgets YAML");
    }

    // -------------------------------------------------------------------------
    // Status group (TestStatusController → TestNoAuthController)
    // -------------------------------------------------------------------------

    @Test
    void statusGroup_returnsHttp200() {
        ResponseEntity<String> response = fetchGroupYaml("e2e-status");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected HTTP 200 for e2e-status group");
    }

    @Test
    void statusGroup_containsGetStatusPath() {
        Map<String, Object> doc = parseYaml(fetchGroupYamlBody("e2e-status"));
        Map<?, ?> paths = (Map<?, ?>) doc.get("paths");
        assertNotNull(paths, "paths section must be present");
        assertTrue(paths.containsKey("/v1/test/status"), "path /v1/test/status must exist in e2e-status group");

        Map<?, ?> statusOps = (Map<?, ?>) paths.get("/v1/test/status");
        assertTrue(statusOps.containsKey("get"), "GET /v1/test/status must be present");
    }

    @Test
    void statusGroup_hasNoSecuritySchemes() {
        Map<String, Object> doc = parseYaml(fetchGroupYamlBody("e2e-status"));
        Map<?, ?> components = (Map<?, ?>) doc.get("components");
        if (components != null) {
            Map<?, ?> securitySchemes = (Map<?, ?>) components.get("securitySchemes");
            assertTrue(securitySchemes == null || securitySchemes.isEmpty(),
                    "e2e-status group must not declare any security schemes (no-auth controller)");
        }
    }

    @Test
    void statusGroup_doesNotContainWidgetsPath() {
        String yaml = fetchGroupYamlBody("e2e-status");
        assertFalse(yaml.contains("/v1/test/widgets"), "e2e-status group must not contain widget paths");
    }

    @Test
    void widgetsGroup_doesNotContainStatusPath() {
        String yaml = fetchGroupYamlBody("e2e-widgets");
        assertFalse(yaml.contains("/v1/test/status"), "e2e-widgets group must not contain status path");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ResponseEntity<String> fetchGroupYaml(String groupName) {
        String url = "http://localhost:" + port + "/v3/api-docs.yaml/" + groupName;
        return restTemplate.getForEntity(url, String.class);
    }

    private String fetchGroupYamlBody(String groupName) {
        ResponseEntity<String> response = fetchGroupYaml(groupName);
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Cannot assert on body: expected HTTP 200 for group " + groupName);
        assertNotNull(response.getBody(), "Response body must not be null for group " + groupName);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(String yamlBody) {
        return new Yaml().load(yamlBody);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> extractSecuritySchemes(Map<String, Object> doc) {
        Map<?, ?> components = (Map<?, ?>) doc.get("components");
        assertNotNull(components, "components section must be present");
        Map<?, ?> securitySchemes = (Map<?, ?>) components.get("securitySchemes");
        assertNotNull(securitySchemes, "components/securitySchemes must be present");
        return securitySchemes;
    }
}
