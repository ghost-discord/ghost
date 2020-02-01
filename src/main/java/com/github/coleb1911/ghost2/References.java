package com.github.coleb1911.ghost2;

import com.github.coleb1911.ghost2.utility.PermanentReference;
import discord4j.core.DiscordClient;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * A collection of globally accessible, commonly used, unchanging objects and constants.
 */
public class References {
    public static final String VERSION_STRING = "1.1";

    private static final PermanentReference<Ghost2Application> APP_INSTANCE = new PermanentReference<>();
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
     * Note: Discord token changes only take effect on restart.
     */
    public static void reloadConfig() {
        final GhostConfig config = CONFIG.get();
        config.reload();
    }

    // Getters
    public static Ghost2Application getApplicationInstance() {
        return APP_INSTANCE.get();
    }

    public static DiscordClient getClient() {
        return CLIENT.get();
    }

    public static GhostConfig getConfig() {
        if (CONFIG.get() == null) {
            GhostConfig config = ConfigFactory.create(GhostConfig.class);
            CONFIG.set(config);
        }

        return CONFIG.get();
    }

    // Setters
    static void setAppInstance(final Ghost2Application appInstance) {
        APP_INSTANCE.set(appInstance);
    }

    static void setClient(final DiscordClient client) {
        CLIENT.set(client);
    }

    static void setStartTime(final long startTimeMs) {
        START_TIME.set(startTimeMs);
    }
}
