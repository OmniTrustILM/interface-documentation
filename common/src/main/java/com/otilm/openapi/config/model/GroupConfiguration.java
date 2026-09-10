package com.otilm.openapi.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

/**
 * Model class representing a single group configuration from groups.yaml
 */
@Getter
@Setter
public class GroupConfiguration {
    private static final String UNKNOWN_GROUP = "<unknown>";
    private String id;
    private String groupName;
    private String title;
    private String description;
    private List<String> interfaces = Collections.emptyList();
    private String serverUrl;
    private Map<String, Object> extensions = Collections.emptyMap();
    private String indexCategory;
    private String navLabel;
    private List<Map<String, List<String>>> security;

    public void setInterfaces(List<String> interfaces) {
        if (interfaces != null) {
            if (interfaces.contains(null)) {
                throw new IllegalArgumentException(
                        "Interfaces list contains null elements for group '" + (id != null ? id : UNKNOWN_GROUP) + "'");
            }
            this.interfaces = List.copyOf(interfaces);
        }
    }

    public void setExtensions(Map<String, Object> extensions) {
        if (extensions != null) {
            if (extensions.containsKey(null) || extensions.containsValue(null)) {
                throw new IllegalArgumentException("Extensions map contains null keys or values for group '"
                        + (id != null ? id : UNKNOWN_GROUP) + "'");
            }
            this.extensions = Map.copyOf(extensions);
        }
    }

    public void setSecurity(List<Map<String, List<String>>> security) {
        if (security != null) {
            if (security.contains(null)) {
                throw new IllegalArgumentException(
                        "Security list contains null elements for group '" + (id != null ? id : UNKNOWN_GROUP) + "'");
            }
            List<Map<String, List<String>>> copy = new ArrayList<>(security.size());
            for (Map<String, List<String>> requirement : security) {
                if (requirement.containsKey(null)) {
                    throw new IllegalArgumentException("Security requirement map contains null keys for group '"
                            + (id != null ? id : UNKNOWN_GROUP) + "'");
                }
                Map<String, List<String>> reqCopy = LinkedHashMap.newLinkedHashMap(requirement.size());
                for (Map.Entry<String, List<String>> entry : requirement.entrySet()) {
                    List<String> scopes = getScopes(entry);
                    reqCopy.put(entry.getKey(), List.copyOf(scopes));
                }
                copy.add(Map.copyOf(reqCopy));
            }
            this.security = List.copyOf(copy);
        }
    }

    private @NonNull List<String> getScopes(Map.Entry<String, List<String>> entry) {
        List<String> scopes = entry.getValue();
        if (scopes == null) {
            throw new IllegalArgumentException("Security requirement map contains null scope list for scheme '"
                    + entry.getKey() + "' in group '" + (id != null ? id : UNKNOWN_GROUP) + "'");
        }
        if (scopes.contains(null)) {
            throw new IllegalArgumentException("Security scope list contains null elements for scheme '"
                    + entry.getKey() + "' in group '" + (id != null ? id : UNKNOWN_GROUP) + "'");
        }
        return scopes;
    }
}
