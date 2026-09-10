package com.otilm.openapi.codegen.testdata.security;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@SecurityRequirements({@SecurityRequirement(name = "ClassLevelScheme")})
public class LegacyController {
    @SecurityRequirement(name = "MethodLevelScheme")
    public void someMethod() {
    }
}
