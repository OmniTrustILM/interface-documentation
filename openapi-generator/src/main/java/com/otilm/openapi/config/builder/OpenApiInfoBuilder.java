package com.otilm.openapi.config.builder;

import com.otilm.openapi.config.model.CommonConfiguration;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds OpenAPI objects with common configuration elements
 */
@Component
public class OpenApiInfoBuilder {

    /**
     * Builds Info object with common configuration elements
     */
    public Info buildInfo(String title, String description, String version, CommonConfiguration commonConfig) {
        if (title == null) {
            throw new IllegalArgumentException("Title cannot be null");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }

        Info info = new Info().title(title).description(description).version(version);

        addExtensions(info, commonConfig);
        addLicense(info, commonConfig);
        addContact(info, commonConfig);

        return info;
    }

    /**
     * Adds all extensions from common configuration to Info object
     */
    private void addExtensions(Info info, CommonConfiguration commonConfig) {
        if (!commonConfig.getExtensions().isEmpty()) {
            info.extensions(new HashMap<>(commonConfig.getExtensions()));
        }
    }

    /**
     * Adds license to an Info object
     */
    private void addLicense(Info info, CommonConfiguration commonConfig) {
        CommonConfiguration.LicenseConfiguration license = commonConfig.getLicense();
        if (license != null) {
            info.license(new License().name(license.getName()).url(license.getUrl()));
        }
    }

    /**
     * Adds contact to an Info object
     */
    private void addContact(Info info, CommonConfiguration commonConfig) {
        CommonConfiguration.ContactConfiguration contact = commonConfig.getContact();
        if (contact != null) {
            info.contact(new Contact().name(contact.getName()).url(contact.getUrl()).email(contact.getEmail()));
        }
    }

    /**
     * Adds common elements to the OpenAPI object with an optional group-specific server URL override
     */
    public void addCommonElements(OpenAPI openAPI, CommonConfiguration commonConfig, String serverUrlOverride) {
        addServers(openAPI, commonConfig, serverUrlOverride);
        addExternalDocs(openAPI, commonConfig);
    }

    /**
     * Adds servers to OpenAPI object with optional group-specific server URL override If serverUrlOverride is provided,
     * it replaces the URL in common servers while keeping descriptions
     */
    private void addServers(OpenAPI openAPI, CommonConfiguration commonConfig, String serverUrlOverride) {
        List<CommonConfiguration.ServerConfiguration> servers = commonConfig.getServers();

        if (!servers.isEmpty()) {
            List<Server> serverList = servers
                    .stream()
                    .map(serverConfig -> buildServerWithUrlOverride(serverConfig, serverUrlOverride))
                    .toList();
            openAPI.servers(serverList);
        }
    }

    /**
     * Builds a Server object from configuration with optional URL override
     */
    private Server buildServerWithUrlOverride(CommonConfiguration.ServerConfiguration serverConfig,
            String urlOverride) {
        String url = urlOverride != null ? urlOverride : serverConfig.getUrl();
        return new Server().url(url).description(serverConfig.getDescription());
    }

    /**
     * Adds external documentation to OpenAPI object
     */
    private void addExternalDocs(OpenAPI openAPI, CommonConfiguration commonConfig) {
        CommonConfiguration.ExternalDocsConfiguration externalDocs = commonConfig.getExternalDocs();
        if (externalDocs != null) {
            openAPI
                    .externalDocs(new ExternalDocumentation()
                            .description(externalDocs.getDescription())
                            .url(externalDocs.getUrl()));
        }
    }
}
