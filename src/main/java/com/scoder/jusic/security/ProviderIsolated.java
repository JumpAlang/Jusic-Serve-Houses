package com.scoder.jusic.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public entry point that can perform blocking third-party I/O.
 * Pure Redis/in-memory service methods intentionally remain unannotated.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProviderIsolated {
    ProviderWorkload value();
}
