package com.otilm.openapi.test.interfaces.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * Test base interface for auth-protected controllers. Declares two security schemes so the e2e tests can assert they
 * appear in generated OpenAPI documents for groups that use this base class.
 */
@SecuritySchemes({
        @SecurityScheme(name = "TestBearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer",
                bearerFormat = "JWT", description = "Bearer JWT authentication (test)"),
        @SecurityScheme(name = "TestApiKeyAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
                paramName = "X-Test-Api-Key", description = "API key authentication (test)")})
public interface TestAuthProtectedController {
}
