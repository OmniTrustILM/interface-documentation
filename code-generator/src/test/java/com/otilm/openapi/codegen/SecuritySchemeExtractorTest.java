package com.otilm.openapi.codegen;

import com.otilm.openapi.codegen.testdata.security.BaseSecurity1;
import com.otilm.openapi.codegen.testdata.security.BaseSecurity2;
import com.otilm.openapi.codegen.testdata.security.DirectController;
import com.otilm.openapi.codegen.testdata.security.InvalidMultiBaseController;
import com.otilm.openapi.codegen.testdata.security.LegacyController;
import com.otilm.openapi.codegen.testdata.security.MultiBaseTransitiveController;
import com.otilm.openapi.codegen.testdata.security.NoBaseController;
import com.otilm.openapi.codegen.testdata.security.TransitiveController;
import com.otilm.openapi.config.model.SecurityConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecuritySchemeExtractorTest {

    private SecuritySchemeExtractor extractor;
    private static final String BASE1 = BaseSecurity1.class.getName();
    private static final String BASE2 = BaseSecurity2.class.getName();
    private static final String LEGACY = LegacyController.class.getName();

    @BeforeEach
    void setUp() throws ClassNotFoundException {
        SecurityConfiguration config = new SecurityConfiguration(List.of(BASE1, BASE2), List.of(LEGACY));
        extractor = new SecuritySchemeExtractor(config);
    }

    @Test
    void testExtractSchemesFromBaseClasses() {
        List<String> base1Schemes = extractor.getSecuritySchemesForBaseClass(BASE1);
        assertEquals(1, base1Schemes.size());
        assertTrue(base1Schemes.contains("BasicAuth"));

        List<String> base2Schemes = extractor.getSecuritySchemesForBaseClass(BASE2);
        assertEquals(2, base2Schemes.size());
        assertTrue(base2Schemes.contains("BearerAuth"));
        assertTrue(base2Schemes.contains("ApiKey"));
    }

    @Test
    void testExtractSchemesFromLegacyController() {
        List<String> legacySchemes = extractor.getSecuritySchemesForBaseClass(LEGACY);
        // Class level: ClassLevelScheme
        // Method level: MethodLevelScheme
        assertEquals(2, legacySchemes.size());
        assertTrue(legacySchemes.contains("ClassLevelScheme"));
        assertTrue(legacySchemes.contains("MethodLevelScheme"));
    }

    @Test
    void testGetAllBaseClassSchemes() {
        Map<String, SecuritySchemeExtractor.SecuritySchemeInfo> allSchemes = extractor.getAllBaseClassSchemes();
        assertEquals(3, allSchemes.size());
        assertTrue(allSchemes.containsKey(BASE1));
        assertTrue(allSchemes.containsKey(BASE2));
        assertTrue(allSchemes.containsKey(LEGACY));

        assertEquals("BaseSecurity1 → [BasicAuth]", allSchemes.get(BASE1).toString());
    }

    @Test
    void testDetermineBaseSecurityClassDirect() {
        String baseClass = extractor.determineBaseSecurityClass(DirectController.class);
        assertEquals(BASE1, baseClass);
    }

    @Test
    void testDetermineBaseSecurityClassTransitive() {
        String baseClass = extractor.determineBaseSecurityClass(TransitiveController.class);
        assertEquals(BASE1, baseClass);
    }

    @Test
    void testDetermineBaseSecurityClassLegacy() {
        String baseClass = extractor.determineBaseSecurityClass(LegacyController.class);
        assertEquals(LEGACY, baseClass);
    }

    @Test
    void testDetermineBaseSecurityClassInvalidMultiBase() {
        assertThrows(IllegalArgumentException.class,
                () -> extractor.determineBaseSecurityClass(InvalidMultiBaseController.class));
    }

    @Test
    void testDetermineBaseSecurityClassInvalidMultiBaseTransitive() {
        assertThrows(IllegalArgumentException.class,
                () -> extractor.determineBaseSecurityClass(MultiBaseTransitiveController.class));
    }

    @Test
    void testDetermineBaseSecurityClassNoBase() {
        assertThrows(IllegalArgumentException.class,
                () -> extractor.determineBaseSecurityClass(NoBaseController.class));
    }

    @Test
    void testMissingBaseClass() {
        SecurityConfiguration config = new SecurityConfiguration(List.of("com.nonexistent.BaseClass"), List.of());
        assertThrows(ClassNotFoundException.class, () -> new SecuritySchemeExtractor(config));
    }
}
