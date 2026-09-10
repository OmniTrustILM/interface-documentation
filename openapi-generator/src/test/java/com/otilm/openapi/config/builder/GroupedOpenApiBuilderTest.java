package com.otilm.openapi.config.builder;

import com.otilm.openapi.config.model.CommonConfiguration;
import com.otilm.openapi.config.model.GroupConfiguration;
import com.otilm.openapi.config.security.OpenApiSecuritySanitizer;
import com.otilm.openapi.config.security.SecuritySchemeMetadataReader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupedOpenApiBuilderTest {

    @Mock
    private OpenApiInfoBuilder infoBuilder;

    @Mock
    private SecuritySchemeMetadataReader securitySchemeMetadataReader;

    @Mock
    private OpenApiSecuritySanitizer openApiSecuritySanitizer;

    private GroupedOpenApiBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new GroupedOpenApiBuilder(infoBuilder, securitySchemeMetadataReader, openApiSecuritySanitizer,
                "1.0.0");
    }

    @Test
    void shouldApplyEmptySecurityWhenConfiguredWithEmptyList() {
        // Given: Group configuration with explicit empty security
        GroupConfiguration groupConfig = new GroupConfiguration();
        groupConfig.setId("test-group");
        groupConfig.setGroupName("test");
        groupConfig.setTitle("Test API");
        groupConfig.setDescription("Test Description");
        groupConfig.setInterfaces(new ArrayList<>(List.of("com.example.TestController")));
        groupConfig.setSecurity(Collections.emptyList());

        CommonConfiguration commonConfig = new CommonConfiguration();

        // Create OpenAPI with operation-level security
        OpenAPI openApi = new OpenAPI()
                .paths(new Paths()
                        .addPathItem("/v1/test",
                                new PathItem()
                                        .get(new Operation()
                                                .addSecurityItem(new SecurityRequirement().addList("BearerJWTAuth")))));

        // Mock behavior
        when(infoBuilder.buildInfo(anyString(), anyString(), anyString(), any()))
                .thenReturn(new io.swagger.v3.oas.models.info.Info());
        doNothing().when(infoBuilder).addCommonElements(any(), any(), any());

        // When: We call the customizer (simulate what happens during group building)
        // Access the customizer through reflection since it's private
        try {
            var customizerMethod = GroupedOpenApiBuilder.class
                    .getDeclaredMethod("customizeOpenApi", OpenAPI.class, GroupConfiguration.class,
                            CommonConfiguration.class);
            customizerMethod.setAccessible(true);
            customizerMethod.invoke(builder, openApi, groupConfig, commonConfig);
        } catch (Exception e) {
            fail("Failed to invoke customizeOpenApi: " + e.getMessage());
        }

        // Then: Document-level security should be empty list
        assertNotNull(openApi.getSecurity(), "Security should not be null");
        assertTrue(openApi.getSecurity().isEmpty(), "Security should be empty list");

        // And: Operation-level security should be removed
        Operation getOperation = openApi.getPaths().get("/v1/test").getGet();
        assertNull(getOperation.getSecurity(), "Operation-level security should be removed when document-level is []");

        // And: Sanitizer is called with empty set to strip orphan security scheme components
        verify(openApiSecuritySanitizer, times(1)).sanitizeSecuritySchemes(eq(openApi), eq(Collections.emptySet()));
    }

    @Test
    void shouldStripSecuritySchemeComponentsWhenEmptySecurityConfigured() {
        // End-to-end with real sanitizer: security: [] removes orphan scheme definitions
        // AND preserves root security: [] (required by the security-defined lint rule).
        GroupedOpenApiBuilder realSanitizerBuilder = new GroupedOpenApiBuilder(infoBuilder,
                securitySchemeMetadataReader, new OpenApiSecuritySanitizer(), "1.0.0");

        GroupConfiguration groupConfig = new GroupConfiguration();
        groupConfig.setId("test-group");
        groupConfig.setGroupName("test");
        groupConfig.setTitle("Test API");
        groupConfig.setDescription("Test Description");
        groupConfig.setInterfaces(new ArrayList<>(List.of("com.example.TestController")));
        groupConfig.setSecurity(Collections.emptyList());

        OpenAPI openApi = new OpenAPI()
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("BearerJWTAuth", new io.swagger.v3.oas.models.security.SecurityScheme())
                        .addSecuritySchemes("SessionAuth", new io.swagger.v3.oas.models.security.SecurityScheme()))
                .paths(new Paths()
                        .addPathItem("/v1/test",
                                new PathItem()
                                        .get(new Operation()
                                                .addSecurityItem(new SecurityRequirement().addList("BearerJWTAuth")))));

        when(infoBuilder.buildInfo(anyString(), anyString(), anyString(), any()))
                .thenReturn(new io.swagger.v3.oas.models.info.Info());
        doNothing().when(infoBuilder).addCommonElements(any(), any(), any());

        try {
            var m = GroupedOpenApiBuilder.class
                    .getDeclaredMethod("customizeOpenApi", OpenAPI.class, GroupConfiguration.class,
                            CommonConfiguration.class);
            m.setAccessible(true);
            m.invoke(realSanitizerBuilder, openApi, groupConfig, new CommonConfiguration());
        } catch (Exception e) {
            fail("Failed to invoke customizeOpenApi", e);
        }

        assertNull(openApi.getComponents().getSecuritySchemes(),
                "security: [] must strip all security scheme definitions from components");
        assertNotNull(openApi.getSecurity(),
                "Root security: [] must not be nullified (required by security-defined lint rule)");
        assertTrue(openApi.getSecurity().isEmpty(), "Root security must remain an empty list");
    }

    @Test
    void shouldApplyExplicitSecurityRequirements() {
        // Given: Group configuration with explicit security requirements
        GroupConfiguration groupConfig = new GroupConfiguration();
        groupConfig.setId("test-group");
        groupConfig.setGroupName("test");
        groupConfig.setTitle("Test API");
        groupConfig.setDescription("Test Description");
        groupConfig.setInterfaces(new ArrayList<>(List.of("com.example.TestController")));

        // Configure explicit security: [{BearerJWTAuth: []}]
        List<Map<String, List<String>>> security = new ArrayList<>();
        Map<String, List<String>> bearerAuth = new HashMap<>();
        bearerAuth.put("BearerJWTAuth", Collections.emptyList());
        security.add(bearerAuth);
        groupConfig.setSecurity(security);

        CommonConfiguration commonConfig = new CommonConfiguration();
        OpenAPI openApi = new OpenAPI();

        // Mock behavior
        when(infoBuilder.buildInfo(anyString(), anyString(), anyString(), any()))
                .thenReturn(new io.swagger.v3.oas.models.info.Info());
        doNothing().when(infoBuilder).addCommonElements(any(), any(), any());

        // When: Customize OpenAPI
        try {
            var customizerMethod = GroupedOpenApiBuilder.class
                    .getDeclaredMethod("customizeOpenApi", OpenAPI.class, GroupConfiguration.class,
                            CommonConfiguration.class);
            customizerMethod.setAccessible(true);
            customizerMethod.invoke(builder, openApi, groupConfig, commonConfig);
        } catch (Exception e) {
            fail("Failed to invoke customizeOpenApi: " + e.getMessage());
        }

        // Then: Document-level security should contain the requirement
        assertNotNull(openApi.getSecurity());
        assertEquals(1, openApi.getSecurity().size());
        assertTrue(openApi.getSecurity().get(0).containsKey("BearerJWTAuth"));

        // And: Sanitizer should NOT be called
        verify(openApiSecuritySanitizer, never()).sanitizeSecuritySchemes(any(), any());
    }

    @Test
    void shouldUseSanitizerWhenNoExplicitSecurityConfigured() {
        // Given: Group configuration WITHOUT explicit security
        GroupConfiguration groupConfig = new GroupConfiguration();
        groupConfig.setId("test-group");
        groupConfig.setGroupName("test");
        groupConfig.setTitle("Test API");
        groupConfig.setDescription("Test Description");
        groupConfig.setInterfaces(new ArrayList<>(List.of("com.example.TestController")));
        // No security configured (null)

        CommonConfiguration commonConfig = new CommonConfiguration();
        OpenAPI openApi = new OpenAPI();

        // Mock behavior
        when(infoBuilder.buildInfo(anyString(), anyString(), anyString(), any()))
                .thenReturn(new io.swagger.v3.oas.models.info.Info());
        doNothing().when(infoBuilder).addCommonElements(any(), any(), any());
        doNothing().when(openApiSecuritySanitizer).sanitizeSecuritySchemes(any(), any());

        // When: Customize OpenAPI
        try {
            var customizerMethod = GroupedOpenApiBuilder.class
                    .getDeclaredMethod("customizeOpenApi", OpenAPI.class, GroupConfiguration.class,
                            CommonConfiguration.class);
            customizerMethod.setAccessible(true);
            customizerMethod.invoke(builder, openApi, groupConfig, commonConfig);
        } catch (Exception e) {
            fail("Failed to invoke customizeOpenApi: " + e.getMessage());
        }

        // Then: Sanitizer should be called (default behavior)
        verify(openApiSecuritySanitizer, times(1)).sanitizeSecuritySchemes(eq(openApi), any());
    }

    @Test
    void shouldValidateGroupConfiguration() {
        // Given: Group configuration with no interfaces
        GroupConfiguration groupConfig = new GroupConfiguration();
        groupConfig.setId("test-group");
        groupConfig.setGroupName("test");
        groupConfig.setInterfaces(Collections.emptyList());

        CommonConfiguration commonConfig = new CommonConfiguration();

        // When/Then: Should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            builder.buildGroupedOpenApi(groupConfig, commonConfig);
        });
    }
}
