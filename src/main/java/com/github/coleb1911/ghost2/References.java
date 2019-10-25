package com.github.coleb1911.ghost2;

import com.github.coleb1911.ghost2.utility.PermanentReference;
import discord4j.core.DiscordClient;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.pmw.tinylog.Logger;

/**
 * A collection of globally accessible, commonly used, unchanging objects and constants.
 */
public class References {
    public static final String VERSION_STRING = "1.0";

    private static final PermanentReference<Ghost2Application> INSTANCE = new PermanentReference<>();
    private static final PermanentReference<GhostConfig> CONFIG = new PermanentReference<>();
    private static final PermanentReference<DiscordClient> CLIENT = new PermanentReference<>();
    private static final PermanentReference<Long> START_TIME = new PermanentReference<>();

    private References() {
    }

    /**
     * Gets the uptime of the current application instance in a readable format (days, hours, minutes, seconds)
     */
    public static String uptime() {
        return DurationFormatUtils.formatPeriod(START_TIME.get(),
                System.currentTimeMillis(),
                "dd 'days', HH 'hours', mm 'minutes', ss 'seconds'");
    }

    /**
     * Reloads the application config and all related values.<br>
     * Note: The application will exit if a token change occurred. Don't change it at runtime.
     */
    public static void reloadConfig() {
        final GhostConfig config = CONFIG.get();
        final DiscordClient client = CLIENT.get();

        config.reload();
        if (!config.token().equals(client.getConfig().getToken())) {
            Logger.info("Token changed on config reload. Exiting.");
            INSTANCE.get().exit(0);
        }
    }

    // Getters
    public static Ghost2Application getInstance() {
        return INSTANCE.get();
    }

    public static DiscordClient getClient() {
        return CLIENT.get();
    }

    // Setters
    static void setInstance(final Ghost2Application instance) {
        INSTANCE.set(instance);
    }

    public static GhostConfig getConfig() {
        if (CONFIG.get() == null) {
            GhostConfig config = ConfigFactory.create(GhostConfig.class);
            CONFIG.set(config);
        }

        return CONFIG.get();
    }

    static void setClient(final DiscordClient client) {
        CLIENT.set(client);
    }

    static void setStartTime(final long startTimeMs) {
        START_TIME.set(startTimeMs);
    }
}
