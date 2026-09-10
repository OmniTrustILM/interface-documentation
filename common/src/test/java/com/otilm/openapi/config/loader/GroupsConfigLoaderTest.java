package com.otilm.openapi.config.loader;

import com.otilm.openapi.config.model.GroupsConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupsConfigLoaderTest {

    @Test
    void testLoadFromClasspath() throws IOException {
        GroupsConfigLoader loader = new GroupsConfigLoader();
        GroupsConfig config = loader.loadFromClasspath("test-groups.yaml");
        assertNotNull(config);
        assertEquals("Apache 2.0", config.getCommon().getLicense().getName());
    }

    @Test
    void testLoadFromFilesystem() throws Exception {
        GroupsConfigLoader loader = new GroupsConfigLoader();
        Path tempFile = Files.createTempFile("groups-test", ".yaml");
        try {
            String content = "common:\n  license:\n    name: MIT\ngroups: []";
            Files.writeString(tempFile, content);

            GroupsConfig config = loader.loadFromFilesystem(tempFile.toString());
            assertNotNull(config);
            assertEquals("MIT", config.getCommon().getLicense().getName());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testLoadDefault() throws IOException {
        GroupsConfigLoader loader = new GroupsConfigLoader();
        GroupsConfig config = loader.load();
        assertNotNull(config);
    }

    @Test
    void testLoadConfiguration() throws IOException {
        GroupsConfigLoader loader = new GroupsConfigLoader();
        GroupsConfig config = loader.loadFromClasspath("/test-groups.yaml");

        assertNotNull(config);
        assertNotNull(config.getCommon());
        assertEquals("https://example.com/logo.png", config.getCommon().getLogo().getUrl());
        assertEquals("Apache 2.0", config.getCommon().getLicense().getName());
        assertEquals("support@example.com", config.getCommon().getContact().getEmail());
        assertEquals(1, config.getCommon().getServers().size());
        assertEquals("https://api.example.com", config.getCommon().getServers().getFirst().getUrl());

        assertEquals(1, config.getGroups().size());
        assertEquals("group1", config.getGroups().getFirst().getId());
        assertEquals(2, config.getGroups().getFirst().getInterfaces().size());
        assertTrue(config.getGroups().getFirst().getInterfaces().contains("com.example.Interface1"));
        assertEquals("value", config.getGroups().getFirst().getExtensions().get("x-test"));
        Object resolved = config.getGroups().getFirst().getExtensions().get("x-resolved");
        assertInstanceOf(Map.class, resolved);
        assertEquals(1, ((Map<?, ?>) resolved).get("version"));

        assertNotNull(config.getSecurity());
        assertEquals(1, config.getSecurity().baseSecurityInterfaces().size());
        assertEquals("com.otilm.openapi.BaseInterface", config.getSecurity().baseSecurityInterfaces().getFirst());
        assertEquals(1, config.getSecurity().legacyControllers().size());
        assertEquals("com.example.LegacyController", config.getSecurity().legacyControllers().getFirst());
    }

    @Test
    void testLoadEmptySecurity() {
        GroupsConfigLoader loader = new GroupsConfigLoader();
        String yaml = "groups: []";
        Map<String, Object> raw = new org.yaml.snakeyaml.Yaml().load(yaml);
        GroupsConfig config = loader.parse(raw);

        assertNotNull(config.getSecurity());
        assertTrue(config.getSecurity().baseSecurityInterfaces().isEmpty());
        assertTrue(config.getSecurity().legacyControllers().isEmpty());
    }
}
