package com.github.coleb1911.ghost2;

import com.github.coleb1911.ghost2.commands.CommandDispatcher;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

@ComponentScan
@SpringBootApplication
@EnableAutoConfiguration
public class Ghost2Application implements ApplicationRunner {
    private static ConfigurableApplicationContext ctx;
    private static DiscordClient client;
    @Autowired
    private GhostConfig config;
    @Autowired
    private CommandDispatcher dispatcher;
    @Autowired
    private GuildMetaRepository guildRepo;

    public static void main(String[] args) {
        ctx = SpringApplication.run(Ghost2Application.class, args);
    }

    public static void exit(int status) {
        // Log out bot
        client.logout().block();

        // Close Spring application context
        ctx.close();
        SpringApplication.exit(ctx, () -> status);

        // Exit
        Logger.info("exiting");
        System.exit(status);
    }

    @Override
    public void run(ApplicationArguments args) {
        // Set up TinyLog
        String dateString = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH''mm''ss").format(LocalDateTime.now());
        String logFileName = String.format("log/log_%s.txt", dateString);
        Configurator.defaultConfig()
                .level(Level.INFO)
                .addWriter(new FileWriter(logFileName))
                .writingThread(true)
                .activate();

        // Create client
        client = new DiscordClientBuilder(config.getToken())
                .setInitialPresence(Presence.online(Activity.listening("your commands.")))
                .build();

        // Subscribe CommandDispatcher to MessageCreateEvents
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(dispatcher::onMessageEvent);

        // Update guild database with any new guilds
        client.getGuilds()
                .map(Guild::getId)
                .map(Snowflake::asLong)
                .filter(((Predicate<Long>) guildRepo::existsById).negate())
                .map(id -> new GuildMeta(id, GuildMeta.DEFAULT_PREFIX))
                .subscribe(guildRepo::save);


        // Block thread until bot logs out
        client.login().block();
    }
}