package com.otilm.openapi.config.security;

import com.otilm.openapi.codegen.SecuritySchemeCategory;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

/**
 * Reads @SecuritySchemeCategory annotations from generated controller classes. Builds a mapping from base security
 * class to the set of security schemes used by that category.
 */
@Component
public class SecuritySchemeMetadataReader {
    private static final Logger log = LoggerFactory.getLogger(SecuritySchemeMetadataReader.class);
    private static final String GENERATED_PACKAGE = "com.otilm.openapi.generated";

    private final Map<String, Set<String>> baseClassToSchemes = new HashMap<>();

    /**
     * Eagerly initializes the metadata by scanning for @SecuritySchemeCategory annotations. Called by Spring after bean
     * construction.
     */
    @PostConstruct
    public void initialize() {
        log.info("Scanning for @SecuritySchemeCategory annotations in package: {}", GENERATED_PACKAGE);

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SecuritySchemeCategory.class));

        var candidates = scanner.findCandidateComponents(GENERATED_PACKAGE);
        candidates.forEach(this::extractSecuritySchemes);

        log.info("Initialized security scheme metadata for {} base classes", baseClassToSchemes.size());
        for (var entry : baseClassToSchemes.entrySet()) {
            String baseName = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
            log.info("  {} → schemes: {}", baseName, entry.getValue());
        }
    }

    private void extractSecuritySchemes(BeanDefinition candidate) throws IllegalStateException {
        try {
            Class<?> clazz = Class.forName(candidate.getBeanClassName());
            SecuritySchemeCategory annotation = clazz.getAnnotation(SecuritySchemeCategory.class);

            if (annotation != null) {
                String baseClass = annotation.baseClass();
                Set<String> schemes = baseClassToSchemes.computeIfAbsent(baseClass, k -> new HashSet<>());

                Collections.addAll(schemes, annotation.securitySchemes());

                String baseClassName = baseClass.substring(baseClass.lastIndexOf('.') + 1);
                log
                        .debug("Found security metadata: {} → base={}, schemes={}", clazz.getSimpleName(),
                                baseClassName, annotation.securitySchemes());
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class " + candidate.getBeanClassName(), e);
        }
    }

    /**
     * Gets the set of security schemes for all controllers of a given base security class.
     */
    public Set<String> getSchemesForBaseClass(String baseClassFqn) {
        return baseClassToSchemes.getOrDefault(baseClassFqn, new HashSet<>());
    }
}
