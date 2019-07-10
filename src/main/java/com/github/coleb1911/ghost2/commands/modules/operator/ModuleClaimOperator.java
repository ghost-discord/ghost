package com.github.coleb1911.ghost2.commands.modules.operator;

import com.github.coleb1911.ghost2.Ghost2Application;
import com.github.coleb1911.ghost2.GhostConfig;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Random;

public class ModuleClaimOperator extends Module {
    private static final String REPLY_PROMPT = "A key has been generated and logged to the console. Paste it in chat to claim operator. (30s timeout)";
    private static final String REPLY_VALID = "Key valid. Hello, guardian.";
    private static final String REPLY_TIMEOUT = "Operator claim timed out.";

    private static Random rng;

    static {
        try {
            rng = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            Logger.error(e);
        }
    }

    public ModuleClaimOperator() {
        super(new ModuleInfo.Builder(ModuleClaimOperator.class)
                .withName("claimoperator")
                .withDescription("Claim operator for this bot instance"));
    }

    @Override
    public void invoke(CommandContext ctx) {
        // Generate key & fetch app instance
        String key = generateRandomString();
        Ghost2Application app = Ghost2Application.getApplicationInstance();

        // Log key and prompt user for it
        ctx.replyBlocking(REPLY_PROMPT);
        Logger.info("Your key: " + key);

        // Listen for & validate key
        // If valid, the new operator's ID gets saved to ghost.properties and the config values are updated
        app.getClient().getEventDispatcher().on(MessageCreateEvent.class)
                .filter(event -> event.getMember().isPresent())
                .filter(event -> event.getMember().get().getId().equals(ctx.getInvoker().getId()))
                .filter(event -> event.getMessage().getChannelId().equals(ctx.getChannel().getId()))
                .take(1)
                .doOnNext(event -> {
                    if (event.getMessage().getContent().orElse("").equals(key)) {
                        ctx.reply(REPLY_VALID);
                        GhostConfig cfg = Ghost2Application.getApplicationInstance().getConfig();
                        cfg.setProperty("ghost.operatorid", event.getMember().get().getId().asString());
                        URI cfgUri;
                        try {
                            cfgUri = Objects.requireNonNull(Ghost2Application.getApplicationInstance().getClass().getClassLoader().getResource("ghost.properties")).toURI();
                            try (FileOutputStream f = new FileOutputStream(new File(cfgUri), false)) {
                                cfg.store(f, "ghost2 properties");
                                f.flush();
                            }
                        } catch (IOException | URISyntaxException e) {
                            Logger.error(e);
                        }
                    }
                })
                .timeout(Duration.of(30L, ChronoUnit.SECONDS), s -> ctx.reply(REPLY_TIMEOUT))
                .blockFirst();
        app.reloadConfig();
    }

    private String generateRandomString() {
        StringBuilder ret = new StringBuilder();
        String[] validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".split("");
        while (ret.length() < 15) {
            ret.append(validChars[rng.nextInt(validChars.length)]);
        }
        return ret.toString();
    }
}
