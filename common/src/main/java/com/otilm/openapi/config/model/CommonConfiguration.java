package com.otilm.openapi.config.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Model class representing common configuration elements from groups.yaml
 */
@Getter
@Setter
public class CommonConfiguration {
    private LogoConfiguration logo;
    private LicenseConfiguration license;
    private ContactConfiguration contact;
    private ExternalDocsConfiguration externalDocs;
    private List<ServerConfiguration> servers = Collections.emptyList();
    private Map<String, Object> extensions = Collections.emptyMap();

    public void setServers(List<ServerConfiguration> servers) {
        if (servers != null) {
            if (servers.contains(null)) {
                throw new IllegalArgumentException("Servers list contains null elements in common configuration");
            }
            this.servers = List.copyOf(servers);
        }
    }

    public void setExtensions(Map<String, Object> extensions) {
        if (extensions != null) {
            if (extensions.containsKey(null) || extensions.containsValue(null)) {
                throw new IllegalArgumentException(
                        "Extensions map contains null keys or values in common configuration");
            }
            this.extensions = Map.copyOf(extensions);
        }
    }

    @Getter
    @Setter
    public static class LogoConfiguration {
        private String url;
    }

    @Getter
    @Setter
    public static class LicenseConfiguration {
        private String name;
        private String url;
    }

    @Getter
    @Setter
    public static class ContactConfiguration {
        private String name;
        private String url;
        private String email;
    }

    @Getter
    @Setter
    public static class ExternalDocsConfiguration {
        private String description;
        private String url;
    }

    @Getter
    @Setter
    public static class ServerConfiguration {
        private String url;
        private String description;
    }
}
