package com.github.coleb1911.ghost2;

import com.github.coleb1911.ghost2.commands.CommandDispatcher;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.GuildCreateEvent;
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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

/**
 * Application entry point
 */
@ComponentScan
@SpringBootApplication
@EnableAutoConfiguration
public class Ghost2Application implements ApplicationRunner {
    private static final String MESSAGE_SET_OPERATOR = "No operator has been set for this bot instance. Use the \'claimoperator\' command to set one; until then, operator commands won't work.";
    private static final String CONNECTION_ERROR = "General connection error. Check your internet connection and try again.";
    private static final String CONFIG_ERROR = "ghost.properties is missing or does not contain a bot token. Read ghost2's README for info on how to set up the bot.";

    private static Ghost2Application applicationInstance;
    private static ConfigurableApplicationContext ctx;
    private DiscordClient client;
    private long operatorId;
    private GhostConfig config;
    @Autowired
    private CommandDispatcher dispatcher;
    @Autowired
    private GuildMetaRepository guildRepo;

    public static void main(String[] args) {
        ctx = SpringApplication.run(Ghost2Application.class, args);
    }

    public static Ghost2Application getApplicationInstance() {
        return applicationInstance;
    }

    /**
     * Starts the application.
     * <br/>
     * This should only be called by Spring Boot.
     *
     * @param args Arguments passed to the application
     */
    @Override
    public void run(ApplicationArguments args) {
        // Set instance
        applicationInstance = this;

        // Fetch config
        config = ConfigFactory.create(GhostConfig.class);
        String token = config.token();
        if (null == token) {
            Logger.error(CONFIG_ERROR);
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

        // Send MessageCreateEvents to CommandDispatcher
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(e -> {
                    if (e.getMember().isPresent()) {
                        return !e.getMember().get().isBot();
                    }
                    return false;
                })
                .subscribe(dispatcher::onMessageEvent);

        // Add any new guilds to database
        client.getEventDispatcher().on(GuildCreateEvent.class)
                .map(GuildCreateEvent::getGuild)
                .map(Guild::getId)
                .map(Snowflake::asLong)
                .filter(((Predicate<Long>) guildRepo::existsById).negate())
                .map(id -> new GuildMeta(id, GuildMeta.DEFAULT_PREFIX))
                .subscribe(guildRepo::save);

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
                        Logger.error(CONNECTION_ERROR);
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

        // Close Spring application context
        SpringApplication.exit(ctx, () -> status);

        // Exit
        Logger.info("exiting");
        System.exit(status);
    }

    /**
     * Reloads the application config & all related values.
     * <br/>
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

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    public long getOperatorId() {
        return operatorId;
    }

    public GhostConfig getConfig() {
        return config;
    }
}