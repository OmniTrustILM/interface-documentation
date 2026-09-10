package com.otilm.openapi.config.loader;

import com.otilm.openapi.config.model.CommonConfiguration;
import com.otilm.openapi.config.model.GroupConfiguration;
import com.otilm.openapi.config.model.GroupsConfig;
import com.otilm.openapi.config.model.SecurityConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Centralized loader for groups.yaml configuration. Merges functionality from codegen and openapi-generator.
 */
public class GroupsConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(GroupsConfigLoader.class);

    public static final String GROUPS_YAML = "groups.yaml";
    public static final List<String> GROUPS_YAML_FILESYSTEM_PATHS = List
            .of(GROUPS_YAML, ".." + File.separator + GROUPS_YAML);

    private final ExtensionReferenceResolver extensionReferenceResolver = new ExtensionReferenceResolver();
    private final boolean resolveExtensions;

    public GroupsConfigLoader() {
        this(true);
    }

    private GroupsConfigLoader(boolean resolveExtensions) {
        this.resolveExtensions = resolveExtensions;
    }

    /**
     * Returns a loader that skips extension resolution. Use this when the API interface classes referenced by
     * extensions are not available on the classpath (e.g. in the index.html generator).
     */
    public static GroupsConfigLoader withoutExtensionResolution() {
        return new GroupsConfigLoader(false);
    }

    /**
     * Loads the configuration from the first available location. Tries filesystem paths, then classpath.
     */
    public GroupsConfig load() throws IOException {
        // 1. Try from filesystem paths
        for (String path : GROUPS_YAML_FILESYSTEM_PATHS) {
            GroupsConfig config = loadFromFilesystem(path);
            if (config != null) {
                return config;
            }
        }

        // 2. Try from the classpath
        GroupsConfig config = loadFromClasspath(GROUPS_YAML);
        if (config != null) {
            return config;
        }

        throw new IllegalStateException(
                "Cannot find " + GROUPS_YAML + " configuration file in filesystem or classpath");
    }

    /**
     * Loads the configuration from the filesystem. Returns null if the file does not exist.
     */
    public GroupsConfig loadFromFilesystem(String path) throws IOException {
        Path filePath = Paths.get(path);
        if (Files.exists(filePath)) {
            try (InputStream is = Files.newInputStream(filePath)) {
                logger.info("Loading configuration from filesystem: {}", filePath.toAbsolutePath());
                return load(is);
            } catch (IOException e) {
                throw new IOException("Failed to load " + GROUPS_YAML + " configuration from " + path, e);
            }
        }
        return null;
    }

    /**
     * Loads the configuration from the classpath. Returns null if the resource does not exist.
     */
    public GroupsConfig loadFromClasspath(String resourceName) throws IOException {
        String normalizedPath = resourceName.startsWith("/") ? resourceName : "/" + resourceName;
        try (InputStream is = getClass().getResourceAsStream(normalizedPath)) {
            if (is != null) {
                logger.info("Loading configuration from classpath: {}", normalizedPath);
                return load(is);
            }
        } catch (IOException e) {
            throw new IOException("Failed to load " + GROUPS_YAML + " configuration from classpath: " + normalizedPath,
                    e);
        }
        return null;
    }

    /**
     * Loads the configuration from an InputStream.
     */
    public GroupsConfig load(InputStream inputStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> rawConfig = yaml.load(inputStream);
        GroupsConfig config = parse(rawConfig);
        if (resolveExtensions) {
            resolveExtensions(config);
        }
        return config;
    }

    private void resolveExtensions(GroupsConfig config) {
        if (!config.getCommon().getExtensions().isEmpty()) {
            config
                    .getCommon()
                    .setExtensions(
                            resolveTopLevelExtensions(config.getCommon().getExtensions(), "common configuration"));
        }
        for (GroupConfiguration group : config.getGroups()) {
            if (!group.getExtensions().isEmpty()) {
                group.setExtensions(resolveTopLevelExtensions(group.getExtensions(), "group " + group.getId()));
            }
        }
    }

    private Map<String, Object> resolveTopLevelExtensions(Map<String, Object> extensions, String contextLabel) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : extensions.entrySet()) {
            resolved
                    .put(entry.getKey(), extensionReferenceResolver
                            .resolveTopLevelValue(entry.getValue(), entry.getKey(), contextLabel));
        }
        return resolved;
    }

    /**
     * Parses the raw configuration Map into a GroupsConfig object.
     */
    @SuppressWarnings("unchecked")
    public GroupsConfig parse(Map<String, Object> rawConfig) {
        if (rawConfig == null) {
            throw new IllegalArgumentException("Raw configuration map cannot be null");
        }

        GroupsConfig config = new GroupsConfig();

        // Parse common
        Map<String, Object> rawCommon = (Map<String, Object>) rawConfig.get("common");
        config.setCommon(parseCommon(rawCommon));

        // Parse groups
        List<Map<String, Object>> rawGroups = (List<Map<String, Object>>) rawConfig.get("groups");
        config.setGroups(parseGroups(rawGroups));

        // Parse security
        Map<String, Object> rawSecurity = (Map<String, Object>) rawConfig.get("security");
        config.setSecurity(parseSecurity(rawSecurity));

        return config;
    }

    @SuppressWarnings("unchecked")
    private CommonConfiguration parseCommon(Map<String, Object> rawCommon) {
        if (rawCommon == null) {
            return new CommonConfiguration();
        }

        CommonConfiguration common = new CommonConfiguration();

        // Logo
        Map<String, Object> logoMap = (Map<String, Object>) rawCommon.get("logo");
        if (logoMap != null) {
            CommonConfiguration.LogoConfiguration logo = new CommonConfiguration.LogoConfiguration();
            logo.setUrl((String) logoMap.get("url"));
            common.setLogo(logo);
        }

        // License
        Map<String, Object> licenseMap = (Map<String, Object>) rawCommon.get("license");
        if (licenseMap != null) {
            CommonConfiguration.LicenseConfiguration license = new CommonConfiguration.LicenseConfiguration();
            license.setName((String) licenseMap.get("name"));
            license.setUrl((String) licenseMap.get("url"));
            common.setLicense(license);
        }

        // Contact
        Map<String, Object> contactMap = (Map<String, Object>) rawCommon.get("contact");
        if (contactMap != null) {
            CommonConfiguration.ContactConfiguration contact = new CommonConfiguration.ContactConfiguration();
            contact.setName((String) contactMap.get("name"));
            contact.setUrl((String) contactMap.get("url"));
            contact.setEmail((String) contactMap.get("email"));
            common.setContact(contact);
        }

        // ExternalDocs
        Map<String, Object> externalDocsMap = (Map<String, Object>) rawCommon.get("externalDocs");
        if (externalDocsMap != null) {
            CommonConfiguration.ExternalDocsConfiguration externalDocs = new CommonConfiguration.ExternalDocsConfiguration();
            externalDocs.setDescription((String) externalDocsMap.get("description"));
            externalDocs.setUrl((String) externalDocsMap.get("url"));
            common.setExternalDocs(externalDocs);
        }

        // Servers
        List<Map<String, Object>> serversMap = (List<Map<String, Object>>) rawCommon.get("servers");
        if (serversMap != null) {
            List<CommonConfiguration.ServerConfiguration> servers = serversMap.stream().map(this::parseServer).toList();
            common.setServers(servers);
        }

        // Extensions
        Map<String, Object> extensionsMap = (Map<String, Object>) rawCommon.get("extensions");
        if (extensionsMap != null) {
            common.setExtensions(extensionsMap);
        }

        return common;
    }

    private CommonConfiguration.ServerConfiguration parseServer(Map<String, Object> serverMap) {
        CommonConfiguration.ServerConfiguration server = new CommonConfiguration.ServerConfiguration();
        server.setUrl((String) serverMap.get("url"));
        server.setDescription((String) serverMap.get("description"));
        return server;
    }

    private List<GroupConfiguration> parseGroups(List<Map<String, Object>> rawGroups) {
        if (rawGroups == null) {
            logger.warn("No groups found in configuration");
            return Collections.emptyList();
        }

        return rawGroups.stream().map(this::parseGroup).toList();
    }

    @SuppressWarnings("unchecked")
    private GroupConfiguration parseGroup(Map<String, Object> groupMap) {
        GroupConfiguration group = new GroupConfiguration();
        group.setId((String) groupMap.get("id"));
        group.setGroupName((String) groupMap.get("groupName"));
        group.setTitle((String) groupMap.get("title"));
        group.setDescription((String) groupMap.get("description"));
        group.setInterfaces((List<String>) groupMap.get("interfaces"));
        group.setServerUrl((String) groupMap.get("serverUrl"));
        group.setExtensions((Map<String, Object>) groupMap.get("extensions"));
        group.setIndexCategory((String) groupMap.get("indexCategory"));
        group.setSecurity((List<Map<String, List<String>>>) groupMap.get("security"));
        return group;
    }

    @SuppressWarnings("unchecked")
    private SecurityConfiguration parseSecurity(Map<String, Object> rawSecurity) {
        if (rawSecurity == null) {
            return new SecurityConfiguration(Collections.emptyList(), Collections.emptyList());
        }

        List<String> baseSecurityInterfaces = (List<String>) rawSecurity.get("baseSecurityInterfaces");
        List<String> legacyControllers = (List<String>) rawSecurity.get("legacyControllers");

        return new SecurityConfiguration(baseSecurityInterfaces, legacyControllers);
    }
}
