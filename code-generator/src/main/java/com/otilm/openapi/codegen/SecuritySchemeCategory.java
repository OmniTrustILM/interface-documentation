package com.otilm.openapi.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata annotation placed on generated dummy controller classes. Records which base security controller the
 * interface extends and which security schemes are applicable.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuritySchemeCategory {

    /**
     * The fully qualified name of the base security controller class that the interface extends. This should be one of
     * the configured base security interfaces, or the legacy com.otilm.api.interfaces.core.web.InfoController
     * exception.
     */
    String baseClass();

    /**
     * The names of security schemes applicable to this controller. Extracted from @SecurityScheme annotations on the
     * base class.
     */
    String[] securitySchemes() default {};
}
