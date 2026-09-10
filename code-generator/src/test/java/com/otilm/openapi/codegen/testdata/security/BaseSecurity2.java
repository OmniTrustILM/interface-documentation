package com.otilm.openapi.codegen.testdata.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

@SecuritySchemes({
        @SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer"),
        @SecurityScheme(name = "ApiKey", type = SecuritySchemeType.APIKEY)})
public interface BaseSecurity2 {
}
