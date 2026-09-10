package com.otilm.openapi.test.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "System status response")
public class StatusDto {

    @Schema(description = "Status message", example = "OK")
    private String status;

    @Schema(description = "API version", example = "test")
    private String version;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
