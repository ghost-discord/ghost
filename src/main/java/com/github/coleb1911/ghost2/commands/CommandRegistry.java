package com.github.coleb1911.ghost2.commands;

import com.github.coleb1911.ghost2.commands.meta.Command;
import org.reflections.Reflections;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class CommandRegistry {
    private static final String MODULE_PACKAGE = "com.github.coleb1911.ghost2.commands.modules";

    private Set<Class<? extends Command>> modules;
    private List<Command> instantiated;

    CommandRegistry() throws ReflectiveOperationException {
        // Create instantiated object "cache"
        instantiated = new LinkedList<>();

        // Find all Command modules
        Reflections reflector = new Reflections(MODULE_PACKAGE);
        modules = reflector.getSubTypesOf(Command.class);

        // Construct an instance of each module
        for (Class<? extends Command> module : modules) {
            instantiated.add(module.newInstance());
        }
    }

    Command getCommandInstance(String name) throws ReflectiveOperationException {
        for (Command command : instantiated) {
            if (name.equals(command.getName())) {
                instantiated.remove(command);
                instantiated.add(command.getClass().newInstance());
                return command;
            }
        }
        return null;
    }
}
