package com.github.coleb1911.ghost2.commands;

import com.github.coleb1911.ghost2.commands.meta.EventHandler;
import com.github.coleb1911.ghost2.commands.meta.InvalidModuleException;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.Event;
import org.pmw.tinylog.Logger;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
public final class CommandRegistry {
    private static final String MODULE_PACKAGE = CommandRegistry.class.getPackageName();

    private final List<Module> instances = new ArrayList<>();
    private final Set<Class<?>> invalidModules = new LinkedHashSet<>();
    private final AutowireCapableBeanFactory beanFactory;

    @Autowired
    @ReflectiveAccess
    public CommandRegistry(ApplicationContext context) {
        this.beanFactory = context.getAutowireCapableBeanFactory();

        Reflections reflections = new Reflections(MODULE_PACKAGE);
        Set<Class<? extends Module>> moduleClasses = reflections.getSubTypesOf(Module.class);
        for (Class<? extends Module> moduleClass : moduleClasses) {
            // Make sure module directly extends base class
            if (!Module.class.equals(moduleClass.getSuperclass())) {
                Logger.warn(moduleClass.getSimpleName() + " does not directly extend Module. It will be excluded from the command list.");
                continue;
            }

            // Construct instance
            try {
                Module instance = createInstance(moduleClass);
                instances.add(instance);
            } catch (InvalidModuleException e) {
                Logger.error(e.getMessage());
                invalidModules.add(moduleClass);
            }
        }
    }

    /**
     * Gets a {@link Module} instance by name.
     *
     * @param name Command name
     * @return Command instance, or null if no command with that name exists
     */
    Optional<Module> getCommandInstance(@NonNull String name) {
        for (Module module : instances) {
            ModuleInfo info = module.getInfo();
            if (name.equals(info.getName()) || info.getAliases().contains(name)) {
                instances.remove(module);
                instances.add(createInstance(module.getClass()));
                beanFactory.autowireBean(module);
                return Optional.of(module);
            }
        }
        return Optional.empty();
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
    Set<Class<?>> getInvalidModules() {
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
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new InvalidModuleException(moduleClass, InvalidModuleException.Reason.NOT_INSTANTIABLE);
        } catch (InvocationTargetException e) {
            throw new InvalidModuleException(moduleClass, e.getTargetException());
        }
        return ret;
    }

    /**
     * Called when the Discord client is ready. Registers event listeners for all discovered modules.
     * <p>
     * This method is for Ghost2Application only. Do not call it.
     */
    public void registerEventListeners(DiscordClient client) {
        for (Module instance : instances) {
            // Read all the module's public methods
            for (Method method : instance.getClass().getMethods()) {
                // Get parameter type for this method
                Optional<Class<? extends Event>> eventType = Optional.ofNullable(method.getAnnotation(EventHandler.class)).map(EventHandler::value);
                if (eventType.isEmpty()) continue;

                // Make sure module has been wired
                beanFactory.autowireBean(instance);

                // Register the listener
                client.getEventDispatcher().on(eventType.get())
                        .subscribe(cEvent -> {
                            try {
                                method.invoke(instance, cEvent);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                Logger.error(instance.getClass().getSimpleName() + " event handler errored on invocation: " + e.getCause().getMessage());
                            }
                        });
            }
        }
    }
}