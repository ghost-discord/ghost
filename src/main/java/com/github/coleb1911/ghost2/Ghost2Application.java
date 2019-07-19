package com.github.coleb1911.ghost2;

import com.github.coleb1911.ghost2.commands.CommandDispatcher;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import org.aeonbits.owner.ConfigFactory;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.FileWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Application entry point
 */
@ComponentScan
@SpringBootApplication
@EnableAutoConfiguration
@EnableJpaRepositories("com.github.coleb1911.ghost2.database.repos")
public class Ghost2Application implements ApplicationRunner {
    private static final String MESSAGE_SET_OPERATOR = "No operator has been set for this bot instance. Use the \'claimoperator\' command to set one; until then, operator commands won't work.";
    private static final String ERROR_CONNECTION = "General connection error. Check your internet connection and try again.";
    private static final String ERROR_CONFIG = "ghost.properties is missing or does not contain a bot token. Read ghost2's README for info on how to set up the bot.";

    private static Ghost2Application applicationInstance;

    @Autowired private ApplicationContext ctx;
    @Autowired private CommandDispatcher dispatcher;
    @Autowired private GuildMetaRepository guildRepo;
    private DiscordClient client;
    private GhostConfig config;
    private RandomAccessFile lockFile;
    private FileLock lock;
    private long operatorId;

    public static void main(String[] args) {
        SpringApplication.run(Ghost2Application.class, args);
    }

    /**
     * @return Main application class instance
     */
    public static Ghost2Application getApplicationInstance() {
        return applicationInstance;
    }

    /**
     * Starts the application.<br>
     * This should only be called by Spring Boot.
     *
     * @param args Arguments passed to the application
     */
    @Override
    public void run(ApplicationArguments args) {
        // Try to acquire a lock to ensure only one application instance is running
        if (!this.lock()) {
            Logger.error("Only one instance of ghost2 may run at a time. Close any other instances and try again.");
            exit(1);
        }

        // Set instance
        applicationInstance = this;

        // Fetch config
        config = ConfigFactory.create(GhostConfig.class);
        String token = config.token();
        if (null == token) {
            Logger.error(ERROR_CONFIG);
            return;
        }

        // Set up TinyLog
        String dateString = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH''mm''ss").format(LocalDateTime.now());
        String logFileName = String.format("log/log_%s.txt", dateString);
        Configurator.defaultConfig()
                .level(args.containsOption("debug") ? Level.DEBUG : Level.INFO)
                .addWriter(new FileWriter(logFileName))
                .writingThread(true)
                .activate();

        // Create client
        client = new DiscordClientBuilder(token)
                .setInitialPresence(Presence.online(Activity.listening("your commands.")))
                .build();

        // Register event listeners
        this.registerListeners(client);

        // Get current bot operator, log notice if null
        operatorId = config.operatorId();
        if (operatorId == -1) {
            Logger.info(MESSAGE_SET_OPERATOR);
        }

        // Log in and block main thread until bot logs out
        client.login()
                .retry(5L)
                .doOnError(throwable -> {
                    if (throwable instanceof IOException) {
                        Logger.error(ERROR_CONNECTION);
                        exit(1);
                    }
                }).block();
    }

    /**
     * Closes all resources, logs out the bot, and terminates the application gracefully.
     *
     * @param status Status code
     */
    public void exit(int status) {
        // Log out bot
        client.logout().block();

        // Release application lock
        this.unlock();

        // Close Spring application context
        SpringApplication.exit(ctx, () -> status);
        Logger.info("Exiting.");
    }

    /**
     * Reloads the application config &amp; all related values.<br>
     * Note: The application will exit if a token change occurred. Don't change it at runtime.
     */
    public void reloadConfig() {
        config.reload();
        if (!config.token().equals(client.getConfig().getToken())) {
            Logger.info("Token changed on config reload. Exiting.");
            exit(0);
            return;
        }
        operatorId = config.operatorId();
    }

    /**
     * @return The {@code CommandDispatcher} for this instance
     */
    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * @return The user ID of the bot's current operator
     */
    public long getOperatorId() {
        return operatorId;
    }

    /**
     * @return The currently loaded application config
     */
    public GhostConfig getConfig() {
        return config;
    }

    /**
     * Tries to acquire a lock on ghost2.lock
     *
     * @return True if lock was successfully acquired
     */
    private boolean lock() {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            lockFile = new RandomAccessFile(tempDir + File.separator + "ghost2.lock", "rw");
            lock = lockFile.getChannel().tryLock();
            return lock != null;
        } catch (IOException e) {
            Logger.error("Failed to acquire lock on ghost2.lock", e);
            return false;
        }
    }

    /**
     * Releases the lock on ghost2.lock
     */
    private void unlock() {
        try {
            lock.release();
            lockFile.close();
        } catch (IOException e) {
            Logger.error("Failed to release lock on ghost2.lock", e);
        }
    }

    /**
     * Registers all the event listeners ghost2 needs
     *
     * @param client Client object to register listeners on
     */
    private void registerListeners(DiscordClient client) {
        // Drop guilds we were removed from when offline (if any)
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(ignore ->
                        client.getGuilds()
                                .map(Guild::getId)
                                .map(Snowflake::asLong)
                                .filter(((Predicate<Long>) guildRepo::existsById).negate())
                                .subscribe(guildRepo::deleteById)
                );

        // Add new guilds to database as they're received
        client.getEventDispatcher().on(GuildCreateEvent.class)
                .map(GuildCreateEvent::getGuild)
                .map(Guild::getId)
                .map(Snowflake::asLong)
                .filter(((Predicate<Long>) guildRepo::existsById).negate())
                .map(id -> new GuildMeta(id, GuildMeta.DEFAULT_PREFIX))
                .doOnEach(signal -> Logger.debug("Added new guild {} to database", Objects.requireNonNull(signal.get()).getId()))
                .subscribe(guildRepo::save);

        // Drop guilds when we're removed from them
        client.getEventDispatcher().on(GuildDeleteEvent.class)
                .filter(e -> !e.isUnavailable())
                .map(GuildDeleteEvent::getGuild)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Guild::getId)
                .map(Snowflake::asLong)
                .filter(guildRepo::existsById)
                .doOnEach(signal -> Logger.debug("Removed guild {} from database", signal.get()))
                .subscribe(guildRepo::deleteById);

        // Autorole
        client.getEventDispatcher().on(MemberJoinEvent.class)
                .subscribe(e -> {
                    GuildMeta meta = guildRepo.findById(e.getGuildId().asLong()).orElseThrow();
                    if (meta.getAutoRoleEnabled() && !meta.getAutoRoleConfirmationEnabled()) {
                        e.getMember().addRole(Snowflake.of(meta.getAutoRoleId()), "Autorole").subscribe();
                        String dm = String.format("Welcome to %s! You've been given your role automatically.", Objects.requireNonNull(e.getGuild().block()).getName());
                        e.getMember().getPrivateChannel().subscribe(c -> c.createMessage(dm).subscribe());
                    }
                });

        // Send MessageCreateEvents to CommandDispatcher
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> {
                    if (e.getMember().isPresent()) {
                        return !e.getMember().get().isBot();
                    }
                    return false;
                })
                .subscribe(dispatcher::onMessageEvent);
    }
}