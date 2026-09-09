package com.otilm.openapi.config.security;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sanitizes OpenAPI specifications by removing unwanted security schemas.
 * <p>
 * Takes a set of allowed security scheme names and: 1. Removes all unwanted schemes from components.securitySchemes 2.
 * Removes all references to deleted schemes from operation-level and global-level security arrays
 */
@Component
public class OpenApiSecuritySanitizer {
    private static final Logger log = LoggerFactory.getLogger(OpenApiSecuritySanitizer.class);

    /**
     * Removes unwanted security schemes from the OpenAPI object. Keeps only those in the allowedSchemes set.
     */
    public void sanitizeSecuritySchemes(OpenAPI openApi, Set<String> allowedSchemes) {
        if (openApi == null) {
            return;
        }

        Set<String> validSchemes = allowedSchemes == null ? Collections.emptySet() : allowedSchemes;

        // 1. Sanitize components/securitySchemes
        if (openApi.getComponents() != null && openApi.getComponents().getSecuritySchemes() != null) {
            Map<String, SecurityScheme> securitySchemes = openApi.getComponents().getSecuritySchemes();
            securitySchemes.keySet().removeIf(schemeName -> !validSchemes.contains(schemeName));

            // Avoid emitting an empty map as `securitySchemes: {}` in YAML output.
            if (securitySchemes.isEmpty()) {
                openApi.getComponents().setSecuritySchemes(null);
            }
        }

        // 2. Remove references from global security
        if (openApi.getSecurity() != null) {
            boolean originallyEmpty = openApi.getSecurity().isEmpty();
            var filteredGlobalSecurity = new ArrayList<>(openApi.getSecurity());
            filteredGlobalSecurity.removeIf(secReq -> !isValidSecurityRequirement(secReq, validSchemes));
            // Preserve explicit security: [] — same semantics as operation-level handling above.
            if (filteredGlobalSecurity.isEmpty() && !originallyEmpty) {
                openApi.setSecurity(null);
            } else {
                openApi.setSecurity(filteredGlobalSecurity);
            }
        }

        // 3. Remove references to deleted schemes from all operations
        if (openApi.getPaths() != null) {
            openApi.getPaths().values().forEach(pathItem -> removeInvalidSecurityRequirements(pathItem, validSchemes));
        }

        log.debug("Sanitized security schemes. Kept: {}", validSchemes);
    }

    /**
     * Removes security requirements from a path item that reference deleted schemes.
     */
    private void removeInvalidSecurityRequirements(PathItem pathItem, Set<String> validSchemeNames) {
        if (pathItem.readOperationsMap() == null) {
            return;
        }
        for (Operation operation : pathItem.readOperationsMap().values()) {
            if (operation.getSecurity() != null) {
                boolean originallyEmpty = operation.getSecurity().isEmpty();
                var filteredOperationSecurity = new ArrayList<>(operation.getSecurity());
                filteredOperationSecurity.removeIf(secReq -> !isValidSecurityRequirement(secReq, validSchemeNames));
                // Preserve an explicitly empty security list (security: []) to maintain "no auth required"
                // semantics. Only nullify if the original list was non-empty, but all entries were filtered out.
                if (filteredOperationSecurity.isEmpty() && !originallyEmpty) {
                    operation.setSecurity(null);
                } else {
                    operation.setSecurity(filteredOperationSecurity);
                }
            }
        }
    }

    /**
     * Checks if a security requirement references only valid scheme names. Empty security requirement is valid (means
     * no auth required).
     */
    private boolean isValidSecurityRequirement(SecurityRequirement secReq, Set<String> validSchemeNames) {
        if (secReq == null || secReq.isEmpty()) {
            return true;
        }

        // All schemes in this requirement must be in the valid set
        return validSchemeNames.containsAll(secReq.keySet());
    }
}
