package com.otilm.openapi.test.interfaces;

import com.otilm.openapi.test.interfaces.dto.CreateWidgetRequest;
import com.otilm.openapi.test.interfaces.dto.WidgetDto;
import com.otilm.openapi.test.interfaces.security.TestAuthProtectedController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Test controller interface for widget management. Extends TestAuthProtectedController — operations in this group must
 * carry the TestBearerAuth and TestApiKeyAuth security schemes.
 */
@Tag(name = "Widget Management", description = "Test API for managing widgets")
@RequestMapping("/v1/test/widgets")
public interface TestWidgetController extends TestAuthProtectedController {

    @Operation(summary = "Get a widget by ID")
    @GetMapping("/{id}")
    ResponseEntity<WidgetDto> getWidget(@Parameter(description = "Widget ID") @PathVariable("id") String id);

    @Operation(summary = "Create a widget")
    @PostMapping
    ResponseEntity<WidgetDto> createWidget(@RequestBody CreateWidgetRequest request);

    @Operation(summary = "Delete a widget")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteWidget(@Parameter(description = "Widget ID") @PathVariable("id") String id);
}
