package com.github.coleb1911.ghost2.commands.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to indicate that a class or member is only used reflectively.
 * <p>
 * This is intended to be used to suppress <i>Unused declaration</i> warnings from the IDE when it doesn't detect an
 * injected field or a method that's only used reflectively.
 */
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
public @interface ReflectiveAccess {
}