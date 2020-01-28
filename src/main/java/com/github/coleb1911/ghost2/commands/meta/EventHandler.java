package com.github.coleb1911.ghost2.commands.meta;

import discord4j.core.event.domain.Event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows module classes to register Discord event handlers
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    @ReflectiveAccess
    Class<? extends Event> value();
}
