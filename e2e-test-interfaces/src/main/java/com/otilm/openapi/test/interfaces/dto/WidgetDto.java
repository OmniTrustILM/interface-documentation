package com.otilm.openapi.test.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A test widget")
public class WidgetDto {

    @Schema(description = "Unique identifier", example = "abc-123")
    private String id;

    @Schema(description = "Widget name", example = "My Widget")
    private String name;

    @Schema(description = "Widget description")
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
