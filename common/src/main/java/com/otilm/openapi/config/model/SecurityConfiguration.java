package com.otilm.openapi.config.model;

import java.util.Collections;
import java.util.List;

/**
 * Security configuration loaded from groups.yaml. Contains base security interfaces and legacy controllers.
 */
public record SecurityConfiguration(List<String> baseSecurityInterfaces, List<String> legacyControllers) {
    public SecurityConfiguration {
        if (baseSecurityInterfaces == null) {
            baseSecurityInterfaces = Collections.emptyList();
        }
        if (legacyControllers == null) {
            legacyControllers = Collections.emptyList();
        }
        baseSecurityInterfaces = List.copyOf(baseSecurityInterfaces);
        legacyControllers = List.copyOf(legacyControllers);
    }
}
