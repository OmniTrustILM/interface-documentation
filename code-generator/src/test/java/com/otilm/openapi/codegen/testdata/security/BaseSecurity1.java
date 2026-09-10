package com.otilm.openapi.codegen.testdata.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SecurityScheme(name = "BasicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public interface BaseSecurity1 {
}
