package com.github.coleb1911.ghost2.commands.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides the {@link CommandType} for all {@link Module}s in a package.
 * <br/>
 * This annotation is used by {@link ModuleInfo.Builder} to automatically fill in the type metadata.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface CommandPackage {
    CommandType value();
}
