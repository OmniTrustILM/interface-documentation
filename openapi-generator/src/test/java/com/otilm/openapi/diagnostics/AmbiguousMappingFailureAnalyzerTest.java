package com.otilm.openapi.diagnostics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbiguousMappingFailureAnalyzerTest {

    private AmbiguousMappingFailureAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new AmbiguousMappingFailureAnalyzer();
    }

    @Test
    void analyze_withAmbiguousMappingException_returnsBothConflictingMethods() {
        String springMessage = """
                Error creating bean with name 'requestMappingHandlerMapping': Ambiguous mapping. Cannot map 'myControllerDummyImpl' method
                com.example.MyControllerDummyImpl#createFoo(FooCreateRequest)
                to {POST [/v1/foos], consumes [application/json], produces [application/json]}: There is already 'myControllerDummyImpl' bean method
                com.example.MyControllerDummyImpl#listFoos(SearchRequestDto) mapped.
                """;

        BeanCreationException cause = new BeanCreationException(springMessage);
        FailureAnalysis analysis = analyzer.analyze(new RuntimeException(cause), cause);

        assertNotNull(analysis, "Should produce a FailureAnalysis for ambiguous mapping");
        assertAll(() -> assertTrue(analysis.getDescription().contains("myControllerDummyImpl"), "Should name the bean"),
                () -> assertTrue(analysis.getDescription().contains("listFoos"), "Should list the existing method"),
                () -> assertTrue(analysis.getDescription().contains("createFoo"), "Should list the incoming method"),
                () -> assertTrue(analysis.getDescription().contains("/v1/foos"),
                        "Should include the conflicting path"));
    }

    @Test
    void analyze_withUnrelatedBeanCreationException_returnsNull() {
        BeanCreationException cause = new BeanCreationException("Error creating bean 'foo': some other problem");
        assertNull(analyzer.analyze(new RuntimeException(cause), cause));
    }

    @Test
    void analyze_withNullMessage_returnsNull() {
        BeanCreationException cause = new BeanCreationException(null, (Throwable) null);
        assertNull(analyzer.analyze(new RuntimeException(cause), cause));
    }
}
