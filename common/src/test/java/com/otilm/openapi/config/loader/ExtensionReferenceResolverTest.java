package com.otilm.openapi.config.loader;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionReferenceResolverTest {

    private final ExtensionReferenceResolver resolver = new ExtensionReferenceResolver();

    @Test
    void shouldKeepNonReferenceStringUnchanged() {
        Object resolved = resolver.resolveTopLevelValue("literal-value", "x-sample", "group 'sample'");
        assertEquals("literal-value", resolved);
    }

    @Test
    void shouldResolveBacktickReferenceToMap() {
        Object resolved = resolver
                .resolveTopLevelValue("`com.otilm.openapi.config.loader.fixtures.ExtensionResolverFixtures.VALID_MAP`",
                        "x-metrics-profile", "group 'secret-provider'");

        Map<?, ?> result = assertInstanceOf(Map.class, resolved);
        assertEquals(1, result.get("version"));
        assertInstanceOf(List.class, result.get("required"));
    }

    @Test
    void shouldResolveBacktickReferenceToList() {
        Object resolved = resolver
                .resolveTopLevelValue("`com.otilm.openapi.config.loader.fixtures.ExtensionResolverFixtures.VALID_LIST`",
                        "x-list", "group 'secret-provider'");

        List<?> result = assertInstanceOf(List.class, resolved);
        assertEquals(4, result.size());
        assertEquals("one", result.get(0));
    }

    @Test
    void shouldFailWhenBackticksAreNotWholeValue() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resolver
                        .resolveTopLevelValue(
                                "prefix `com.otilm.openapi.config.loader.fixtures.ExtensionResolverFixtures.VALID_MAP`",
                                "x-metrics-profile", "group 'secret-provider'"));

        assertTrue(ex.getMessage().contains("whole-string references"));
    }

    @Test
    void shouldFailWhenNestedBackticksArePresent() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resolver
                        .resolveTopLevelValue(
                                "`com.otilm.openapi.config.loader.fixtures.ExtensionResolverFixtures.`VALID_MAP``",
                                "x-metrics-profile", "group 'secret-provider'"));

        assertTrue(ex.getMessage().contains("whole-string references"));
    }

    @Test
    void shouldFailWhenReferenceFormatIsInvalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resolver.resolveTopLevelValue("`NotQualified`", "x-metrics-profile", "group 'secret-provider'"));

        assertTrue(ex.getMessage().contains("required format"));
    }

    @Test
    void shouldFailWhenFieldIsNotPublicStatic() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resolver
                .resolveTopLevelValue(
                        "`com.otilm.openapi.config.loader.fixtures.ExtensionResolverFixtures.PRIVATE_STATIC_MAP`",
                        "x-metrics-profile", "group 'secret-provider'"));

        assertTrue(ex.getMessage().contains("public static"));
    }

    @Test
    void shouldFailWhenResolvedTopLevelTypeIsNotMapOrList() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resolver
                        .resolveTopLevelValue(
                                "`com.otilm.openapi.config.loader.fixtures.ExtensionResolverFixtures.INVALID_SCALAR`",
                                "x-metrics-profile", "group 'secret-provider'"));

        assertTrue(ex.getMessage().contains("Only Map and List are supported"));
    }

    @Test
    void shouldFailWhenNestedValueIsNotYamlSafe() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resolver
                .resolveTopLevelValue(
                        "`com.otilm.openapi.config.loader.fixtures.ExtensionResolverFixtures.INVALID_NESTED_OBJECT`",
                        "x-metrics-profile", "group 'secret-provider'"));

        assertTrue(ex.getMessage().contains("Only YAML-safe scalars/maps/lists are supported"));
    }
}
