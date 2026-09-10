package com.otilm.openapi.test.interfaces;

import com.otilm.openapi.test.interfaces.dto.StatusDto;
import com.otilm.openapi.test.interfaces.security.TestNoAuthController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Test controller interface for checking a status. Extends TestNoAuthController — the e2e-test-status group must NOT
 * carry any security schemes in the generated OpenAPI document.
 */
@Tag(name = "Status", description = "Test API for system status")
@RequestMapping("/v1/test/status")
public interface TestStatusController extends TestNoAuthController {

    @Operation(summary = "Get system status")
    @GetMapping
    ResponseEntity<StatusDto> getStatus();
}
