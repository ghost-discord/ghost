package com.github.coleb1911.ghost2.commands;

import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.stream.Collectors;

@Disabled // CommandRegistry constructor changed, need to find a way to fabricate Spring context in test env
class ModuleValidationTest {
    private static final String FAILURE_MESSAGE_FORMAT = "Module validation failed on %d module(s): [%s]";

    private static Constructor<CommandRegistry> constructor;

    @BeforeAll
    @ReflectiveAccess
    static void initAll() throws NoSuchMethodException {
        constructor = CommandRegistry.class.getDeclaredConstructor();
        constructor.setAccessible(true);
    }

    @Test
    @ReflectiveAccess
    void validateModules() throws ReflectiveOperationException {
        CommandRegistry registry = constructor.newInstance();
        Set<Class<?>> failed = registry.getInvalidModules();

        if (!failed.isEmpty()) {
            String failedString = failed.stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));
            Assertions.fail(String.format(FAILURE_MESSAGE_FORMAT, failed.size(), failedString));
        }
    }
}