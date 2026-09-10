package com.otilm.openapi.config.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSecuritySanitizerTest {

    private final OpenApiSecuritySanitizer sanitizer = new OpenApiSecuritySanitizer();

    @Test
    void shouldRemoveEmptySecuritySchemesAfterFilteringAllSchemesOut() {
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSchemas("Dummy", new Schema<>())
                        .addSecuritySchemes("BearerJWTAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP))
                        .addSecuritySchemes("SessionAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY)))
                .paths(new Paths()
                        .addPathItem("/v1/test",
                                new PathItem()
                                        .get(new Operation()
                                                .security(List
                                                        .of(new SecurityRequirement().addList("BearerJWTAuth"),
                                                                new SecurityRequirement().addList("SessionAuth"))))));

        sanitizer.sanitizeSecuritySchemes(openApi, Set.of());

        assertNotNull(openApi.getComponents(), "Components should remain present");
        assertNotNull(openApi.getComponents().getSchemas(), "Unrelated component sections should remain untouched");
        assertNull(openApi.getComponents().getSecuritySchemes(), "Empty securitySchemes map should not be emitted");

        List<SecurityRequirement> operationSecurity = openApi.getPaths().get("/v1/test").getGet().getSecurity();
        assertNull(operationSecurity,
                "Operation with all schemes filtered out should have security field removed (inherits global)");
    }

    @Test
    void shouldPreserveExplicitlyEmptyOperationSecurity() {
        // Operations with security: [] explicitly declare "no authentication required".
        // This is semantically different from an absent security field (which inherits global security).
        OpenAPI openApi = new OpenAPI()
                .paths(new Paths().addPathItem("/v1/public", new PathItem().get(new Operation().security(List.of()))));

        sanitizer.sanitizeSecuritySchemes(openApi, Set.of("BearerJWTAuth"));

        List<SecurityRequirement> operationSecurity = openApi.getPaths().get("/v1/public").getGet().getSecurity();
        assertNotNull(operationSecurity, "Explicitly empty security list (security: []) must not be removed");
        assertTrue(operationSecurity.isEmpty(), "Explicitly empty security list must remain empty");
    }

    @Test
    void shouldPreserveExplicitlyEmptyGlobalSecurity() {
        // Global security: [] means "no auth required" — must not be nullified,
        // unlike a list that had all its schemes filtered out.
        OpenAPI openApi = new OpenAPI().security(List.of());

        sanitizer.sanitizeSecuritySchemes(openApi, Set.of());

        assertNotNull(openApi.getSecurity(), "Explicit root security: [] must not be removed");
        assertTrue(openApi.getSecurity().isEmpty(), "Root security must remain an empty list");
    }

    @Test
    void shouldNullifyGlobalSecurityWhenAllSchemesFilteredOut() {
        // A non-empty security list whose schemes are all removed must be nullified (not preserved).
        OpenAPI openApi = new OpenAPI().addSecurityItem(new SecurityRequirement().addList("RemovedScheme"));

        sanitizer.sanitizeSecuritySchemes(openApi, Set.of());

        assertNull(openApi.getSecurity(), "Global security referencing only removed schemes must be nullified");
    }

    @Test
    void shouldRemovePreExistingEmptySecuritySchemesMap() {
        OpenAPI openApi = new OpenAPI()
                .components(new Components().schemas(Map.of("Dummy", new Schema<>())).securitySchemes(Map.of()));

        sanitizer.sanitizeSecuritySchemes(openApi, Set.of("BearerJWTAuth"));

        assertNull(openApi.getComponents().getSecuritySchemes(), "Pre-existing empty map should be normalized to null");
        assertNotNull(openApi.getComponents().getSchemas(), "Schemas should remain untouched");
    }

    @Test
    void shouldKeepAllowedSecuritySchemes() {
        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("BearerJWTAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP))
                        .addSecuritySchemes("SessionAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY)));

        sanitizer.sanitizeSecuritySchemes(openApi, Set.of("BearerJWTAuth"));

        assertNotNull(openApi.getComponents().getSecuritySchemes());
        assertEquals(Set.of("BearerJWTAuth"), openApi.getComponents().getSecuritySchemes().keySet());
    }

    @Test
    void shouldFilterGlobalSecurity() {
        OpenAPI openApi = new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("BearerJWTAuth"))
                .addSecurityItem(new SecurityRequirement().addList("SessionAuth"));

        sanitizer.sanitizeSecuritySchemes(openApi, Set.of("BearerJWTAuth"));

        assertNotNull(openApi.getSecurity());
        assertEquals(1, openApi.getSecurity().size());
        assertTrue(openApi.getSecurity().get(0).containsKey("BearerJWTAuth"));
    }

    @Test
    void shouldRemoveSecurityRequirementWithMixedValidAndInvalidSchemes() {
        // In OpenAPI, a security requirement (AND logic) is only valid if ALL schemes are present
        OpenAPI openApi = new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("BearerJWTAuth").addList("SessionAuth"));

        sanitizer.sanitizeSecuritySchemes(openApi, Set.of("BearerJWTAuth"));

        assertNull(openApi.getSecurity(),
                "Requirement with an invalid scheme should be completely removed (normalized to null)");
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        assertDoesNotThrow(() -> sanitizer.sanitizeSecuritySchemes(null, Set.of("auth")));

        OpenAPI openApi = new OpenAPI();
        assertDoesNotThrow(() -> sanitizer.sanitizeSecuritySchemes(openApi, null));
    }
}
