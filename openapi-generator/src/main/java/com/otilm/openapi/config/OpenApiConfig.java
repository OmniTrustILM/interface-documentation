package com.otilm.openapi.config;

import com.otilm.openapi.config.builder.GroupedOpenApiBuilder;
import com.otilm.openapi.config.loader.GroupsConfigLoader;
import com.otilm.openapi.config.model.GroupConfiguration;
import com.otilm.openapi.config.model.GroupsConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dynamic OpenAPI configuration that reads group definitions from groups.yaml and creates GroupedOpenApi beans for each
 * group.
 * <p>
 * This class delegates configuration loading and building to specialized components for better separation of concerns
 * and maintainability.
 */
@Configuration
public class OpenApiConfig {
    private final GroupedOpenApiBuilder groupedOpenApiBuilder;
    private final GroupsConfigLoader groupsConfigLoader = new GroupsConfigLoader();

    @Autowired
    public OpenApiConfig(GroupedOpenApiBuilder groupedOpenApiBuilder) {
        this.groupedOpenApiBuilder = groupedOpenApiBuilder;
    }

    /**
     * Creates GroupedOpenApi beans dynamically for each group defined in groups.yaml. Each group is configured to
     * include only the controllers that implement its specified interfaces.
     */
    @Bean
    public List<GroupedOpenApi> groupedOpenApis() throws IOException {
        GroupsConfig config = groupsConfigLoader.load();
        List<GroupedOpenApi> groups = new ArrayList<>();

        for (GroupConfiguration groupConfig : config.getGroups()) {
            try {
                GroupedOpenApi groupedOpenApi = groupedOpenApiBuilder
                        .buildGroupedOpenApi(groupConfig, config.getCommon());
                groups.add(groupedOpenApi);
            } catch (IllegalArgumentException e) {
                // Skip groups with invalid configuration (e.g., no interfaces)
                // Error is already logged in the builder
            }
        }

        return groups;
    }
}
