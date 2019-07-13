package com.github.coleb1911.ghost2.commands;

import com.github.coleb1911.ghost2.commands.meta.InvalidModuleException;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Scans the {@link com.github.coleb1911.ghost2.commands commands} package for valid command {@link Module}s
 * & maintains a {@link List} of instances of each Module.
 * <p/>
 * This class serves two main purposes:
 * <ol>
 * <li>Gets an instance of a Module by name</li>
 * <li>Gets the {@link ModuleInfo} associated with a Module by name</li>
 * </ol>
 */
public class CommandRegistry {
    private static final String MODULE_PACKAGE = "com.github.coleb1911.ghost2.commands.modules";
    private static final String INSTANTIATION_ERROR_FORMAT = "Cannot construct an instance of %s; Module implementations must have a public constructor to function";

    private final List<Module> instantiated;

    public CommandRegistry() {
        // Create instantiated object "cache"
        instantiated = new LinkedList<>();

        // Find all Modules
        Reflections reflector = new Reflections(MODULE_PACKAGE);
        Set<Class<? extends Module>> modules = reflector.getSubTypesOf(Module.class);

        // Construct an instance of each Module
        for (Class<? extends Module> module : modules) {
            try {
                instantiated.add(module.getDeclaredConstructor().newInstance());
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                throw new InvalidModuleException(module, InvalidModuleException.Reason.NOT_INSTANTIABLE);
            } catch (InvocationTargetException e) {
                throw new InvalidModuleException(module, e.getTargetException());
            }
        }
    }

    // TODO: Handle command aliases in CommandRegistry#getCommandInstance and CommandRegistry#getInfo

    /**
     * Gets a {@link Module} instance by name.
     *
     * @param name Command name
     * @return Command instance, or null if no command with that name exists
     */
    Module getCommandInstance(String name) {
        for (Module module : instantiated) {
            if (name.equals(module.getInfo().getName())) {
                instantiated.remove(module);
                try {
                    instantiated.add(module.getClass().getDeclaredConstructor().newInstance());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(String.format(INSTANTIATION_ERROR_FORMAT, instantiated.getClass().getName()));
                }
                return module;
            }
        }
        return null;
    }

    /**
     * Get the {@link ModuleInfo} for a {@link Module} by name
     *
     * @param name Command name
     * @return Associated CommandInfo, or null if no command with that name exists
     * @see #getAllInfo()
     */
    public ModuleInfo getInfo(String name) {
        for (Module module : instantiated) {
            if (name.equals(module.getInfo().getName())) {
                return module.getInfo();
            }
        }
        return null;
    }

    /**
     * Get the {@link ModuleInfo} for every {@link Module} found on the classpath
     *
     * @return Associated CommandInfo for all available Modules
     */
    public List<ModuleInfo> getAllInfo() {
        List<ModuleInfo> ret = new ArrayList<>();
        for (Module module : instantiated) {
            ret.add(module.getInfo());
        }
        return ret;
    }
}