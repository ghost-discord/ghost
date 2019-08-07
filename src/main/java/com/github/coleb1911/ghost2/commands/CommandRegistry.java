package com.github.coleb1911.ghost2.commands;

import com.github.coleb1911.ghost2.commands.meta.InvalidModuleException;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import org.pmw.tinylog.Logger;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
public final class CommandRegistry implements ApplicationListener<ContextRefreshedEvent> {
    private static final String MODULE_PACKAGE = "com.github.coleb1911.ghost2.commands.modules";

    private final List<Module> instances;
    private final Set<Class> invalidModules;
    private AutowireCapableBeanFactory factory;

    @ReflectiveAccess
    public CommandRegistry() {
        // Instantiate lists
        instances = new ArrayList<>();
        invalidModules = new LinkedHashSet<>();

        // Find all Modules
        Reflections reflections = new Reflections(MODULE_PACKAGE);
        Set<Class<? extends Module>> moduleClasses = reflections.getSubTypesOf(Module.class);

        // Construct an instance of each Module
        for (Class<? extends Module> moduleClass : moduleClasses) {
            try {
                instances.add(createInstance(moduleClass));
            } catch (InvalidModuleException e) {
                Logger.error(e.getMessage());
                invalidModules.add(moduleClass);
            }

            // Warn if a Module class isn't final
            // See https://github.com/cbryant02/ghost2/wiki/Writing-a-command-module#notes-and-best-practices
            if (!Modifier.isFinal(moduleClass.getModifiers()))
                Logger.warn("Module {} is not final. Add the final modifier and read the docs to understand why this is bad.", moduleClass.getSimpleName());
        }
    }

    /**
     * Gets a {@link Module} instance by name.
     *
     * @param name Command name
     * @return Command instance, or null if no command with that name exists
     */
    Module getCommandInstance(String name) {
        for (Module module : instances) {
            ModuleInfo info = module.getInfo();
            if (name.equals(info.getName()) || info.getAliases().contains(name)) {
                instances.remove(module);
                instances.add(createInstance(module.getClass()));
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
        for (Module module : instances) {
            if (name.equals(module.getInfo().getName())) {
                return module.getInfo();
            }
        }
        return null;
    }

    /**
     * Get the {@link ModuleInfo} for every {@link Module} found on the classpath.
     * The {@code ModuleInfo} objects are sorted alphabetically by name.
     *
     * @return Associated ModuleInfo for all available Modules
     */
    public List<ModuleInfo> getAllInfo() {
        return instances.stream()
                .map(Module::getInfo)
                .sorted(Comparator.comparing(ModuleInfo::getName))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Gets a Set of invalid Module classes that CommandRegistry found.<br>
     * Used for testing.
     *
     * @return Invalid Module classes found on classpath
     */
    Set<Class> getInvalidModules() {
        return Set.copyOf(invalidModules);
    }

    /**
     * Creates an instance of a Module class.
     *
     * @param moduleClass Desired Module class. <b>Cannot be null.</b>
     * @return New instance of the class, or null if invalid
     * @throws InvalidModuleException if the module is written incorrectly
     * @see InvalidModuleException.Reason
     */
    private Module createInstance(@NotNull Class<? extends Module> moduleClass) throws InvalidModuleException {
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

    /**
     * Listens for ApplicationContext refresh and gets a bean factory when it's available.<br>
     * The bean factory is stored privately and used to autowire Module instances in {@link #createInstance}.
     * <p>
     * This method is for Spring only. Do not call it.
     *
     * @param event ContextRefreshedEvent
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.factory = event.getApplicationContext().getAutowireCapableBeanFactory();
        for (Module module : instances)
            factory.autowireBean(module);
    }
}