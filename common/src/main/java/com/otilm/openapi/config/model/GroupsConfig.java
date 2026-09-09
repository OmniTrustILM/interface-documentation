package com.otilm.openapi.config.model;

import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * Unified model for the entire groups.yaml configuration
 */
@Getter
public class GroupsConfig {
    private CommonConfiguration common = new CommonConfiguration();
    private List<GroupConfiguration> groups = Collections.emptyList();
    private SecurityConfiguration security = new SecurityConfiguration(Collections.emptyList(),
            Collections.emptyList());

    public void setCommon(CommonConfiguration common) {
        if (common != null) {
            this.common = common;
        }
    }

    public void setGroups(List<GroupConfiguration> groups) {
        if (groups != null) {
            this.groups = List.copyOf(groups);
        }
    }

    public void setSecurity(SecurityConfiguration security) {
        if (security != null) {
            this.security = security;
        }
    }
}
