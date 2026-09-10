package com.otilm.openapi.codegen;

import com.otilm.openapi.config.model.SecurityConfiguration;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import jakarta.annotation.Nonnull;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts @SecurityScheme annotations from the base security controller interfaces. Maps each base class to the list
 * of security scheme names it declares.
 */
public class SecuritySchemeExtractor {

    private final List<String> baseSecurityInterfaces;
    private final List<String> legacyControllers;
    private final Map<String, SecuritySchemeInfo> baseClassSchemes = new HashMap<>();

    /**
     * Creates a SecuritySchemeExtractor with configuration loaded from groups.yaml.
     *
     * @param securityConfig the security configuration containing base interfaces and legacy controllers
     */
    public SecuritySchemeExtractor(SecurityConfiguration securityConfig) throws ClassNotFoundException {
        this.baseSecurityInterfaces = new ArrayList<>(securityConfig.baseSecurityInterfaces());
        this.legacyControllers = new ArrayList<>(securityConfig.legacyControllers());
        initializeBaseClassSchemes();
    }

    /**
     * Loads and analyzes the base security controller interfaces. Extracts their @SecurityScheme annotations.
     */
    private void initializeBaseClassSchemes() throws ClassNotFoundException {
        try {
            for (String baseInterface : baseSecurityInterfaces) {
                analyzeBaseClass(baseInterface);
            }
            for (String legacyController : legacyControllers) {
                analyzeLegacyController(legacyController);
            }
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Failed to load base security controller classes: " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes a single base controller class and extracts its security schemes.
     */
    private void analyzeBaseClass(String baseClassFqn) throws ClassNotFoundException {
        Class<?> baseClass = Class.forName(baseClassFqn);
        List<String> schemeNames = new ArrayList<>();

        // Check for @SecuritySchemes annotation (can have multiple schemes)
        SecuritySchemes securitySchemes = baseClass.getAnnotation(SecuritySchemes.class);
        if (securitySchemes != null && securitySchemes.value() != null) {
            for (SecurityScheme scheme : securitySchemes.value()) {
                schemeNames.add(scheme.name());
            }
        }

        // Also check for @SecurityScheme annotation (single scheme)
        SecurityScheme securityScheme = baseClass.getAnnotation(SecurityScheme.class);
        if (securityScheme != null) {
            schemeNames.add(securityScheme.name());
        }

        baseClassSchemes.put(baseClassFqn, new SecuritySchemeInfo(baseClassFqn, schemeNames));
    }

    /**
     * Analyzes a legacy controller that does not extend any base security interface. Extracts security scheme names
     * from @SecurityRequirements annotations.
     */
    private void analyzeLegacyController(String controllerFqn) throws ClassNotFoundException {
        Class<?> controllerClass = Class.forName(controllerFqn);
        List<String> schemeNames = extractSecurityRequirements(controllerClass);

        for (var method : controllerClass.getMethods()) {
            schemeNames.addAll(extractSecurityRequirements(method));
        }

        baseClassSchemes.put(controllerFqn, new SecuritySchemeInfo(controllerFqn, schemeNames));
    }

    private List<String> extractSecurityRequirements(AnnotatedElement element) {
        List<String> schemeNames = new ArrayList<>();

        SecurityRequirements requirements = element.getAnnotation(SecurityRequirements.class);
        if (requirements != null && requirements.value() != null) {
            for (SecurityRequirement requirement : requirements.value()) {
                if (!requirement.name().isBlank()) {
                    schemeNames.add(requirement.name());
                }
            }
        }

        SecurityRequirement requirement = element.getAnnotation(SecurityRequirement.class);
        if (requirement != null && !requirement.name().isBlank()) {
            schemeNames.add(requirement.name());
        }

        return schemeNames;
    }

    /**
     * Determines which base class an interface extends. Validates that it extends exactly one of the base classes,
     * except for legacy controllers, which are explicitly allowed.
     *
     * @param interfaceClass The interface to check
     * @return The FQN of the base security controller class
     * @throws IllegalArgumentException if the interface doesn't inherit from any base class
     */
    public String determineBaseSecurityClass(Class<?> interfaceClass) {
        if (legacyControllers.contains(interfaceClass.getName())) {
            return interfaceClass.getName();
        }

        String matchedBase = findBaseSecurityClass(interfaceClass);

        if (matchedBase == null) {
            throw new IllegalArgumentException("Controller interface " + interfaceClass.getName()
                    + " does not extend any of the base security interfaces: " + baseSecurityInterfaces
                    + ". Legacy exception allowed only for: " + legacyControllers);
        }

        return matchedBase;
    }

    /**
     * Recursively searches for exactly one base security interface in the inheritance hierarchy.
     */
    private String findBaseSecurityClass(Class<?> interfaceClass) {
        String matchedBase = null;

        for (Class<?> iface : interfaceClass.getInterfaces()) {
            String foundBase;
            if (isBaseSecurityClass(iface.getName())) {
                foundBase = iface.getName();
            } else {
                foundBase = findBaseSecurityClass(iface);
            }

            if (foundBase != null) {
                if (matchedBase != null && !matchedBase.equals(foundBase)) {
                    throw new IllegalArgumentException("Interface " + interfaceClass.getName()
                            + " (transitively) extends multiple base security classes: " + matchedBase + " and "
                            + foundBase + ". This is invalid.");
                }
                matchedBase = foundBase;
            }
        }

        return matchedBase;
    }

    /**
     * Checks if a class is one of the base security classes.
     */
    private boolean isBaseSecurityClass(String classFqn) {
        return baseSecurityInterfaces.contains(classFqn);
    }

    /**
     * Gets the security scheme names for a given base class.
     */
    public List<String> getSecuritySchemesForBaseClass(String baseClassFqn) {
        SecuritySchemeInfo info = baseClassSchemes.get(baseClassFqn);
        if (info == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(info.schemeNames);
    }

    /**
     * Gets information about all base classes and their security schemes.
     */
    public Map<String, SecuritySchemeInfo> getAllBaseClassSchemes() {
        return new HashMap<>(baseClassSchemes);
    }

    /**
     * Inner class to hold information about a base class and its security schemes.
     */
    public record SecuritySchemeInfo(String baseClassFqn, List<String> schemeNames) {
        @Nonnull
        public String toString() {
            return baseClassFqn.substring(baseClassFqn.lastIndexOf('.') + 1) + " → " + schemeNames;
        }
    }
}
