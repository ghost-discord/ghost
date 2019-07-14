package com.github.coleb1911.ghost2.commands;

import com.github.coleb1911.ghost2.commands.meta.InvalidModuleException;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Scans the {@link com.github.coleb1911.ghost2.commands commands} package for valid command {@link Module}s
 * &amp; maintains a {@link List} of instances of each Module.
 * <p>
 * This class serves two main purposes:
 * <ol>
 * <li>Gets an instance of a Module by name</li>
 * <li>Gets the {@link ModuleInfo} associated with a Module by name</li>
 * </ol>
 */
@Component
@Configurable
public class CommandRegistry implements ApplicationListener<ContextRefreshedEvent> {
    private static final String MODULE_PACKAGE = "com.github.coleb1911.ghost2.commands.modules";

    private final List<Module> instantiated;
    private AutowireCapableBeanFactory factory;

    @ReflectiveAccess
    CommandRegistry() {
        // Create instantiated object "cache"
        instantiated = new LinkedList<>();

        // Find all Modules
        Reflections reflector = new Reflections(MODULE_PACKAGE);
        Set<Class<? extends Module>> modules = reflector.getSubTypesOf(Module.class);

        // Construct an instance of each Module
        for (Class<? extends Module> module : modules) {
            instantiated.add(createInstance(module));
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
                instantiated.add(createInstance(module.getClass()));
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

    /**
     * Creates an instance of a Module class.
     *
     * @param moduleClass Desired Module class. <b>Cannot be null.</b>
     * @return New instance of the class, or null if invalid
     * @throws InvalidModuleException if the module is written incorrectly
     * @see InvalidModuleException.Reason
     */
    private Module createInstance(@NotNull Class<? extends Module> moduleClass) {
        Module ret;
        try {
            ret = moduleClass.getDeclaredConstructor().newInstance();
            if (factory != null) factory.autowireBean(ret);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new InvalidModuleException(moduleClass, InvalidModuleException.Reason.NOT_INSTANTIABLE);
        } catch (InvocationTargetException e) {
            throw new InvalidModuleException(moduleClass, e.getTargetException());
        }
        return ret;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.factory = event.getApplicationContext().getAutowireCapableBeanFactory();
        for (Module module : instantiated)
            factory.autowireBean(module);
    }
}