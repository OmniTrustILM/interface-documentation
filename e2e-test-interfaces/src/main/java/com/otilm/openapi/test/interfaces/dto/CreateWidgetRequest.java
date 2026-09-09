package com.otilm.openapi.test.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating a widget")
public class CreateWidgetRequest {

    @Schema(description = "Widget name", example = "New Widget", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Widget description")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
