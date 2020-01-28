package com.github.coleb1911.ghost2.commands.modules.operator;

import com.github.coleb1911.ghost2.References;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.database.entities.ApplicationMeta;
import com.github.coleb1911.ghost2.database.repos.ApplicationMetaRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.util.Snowflake;
import org.pmw.tinylog.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import javax.validation.constraints.NotNull;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Configurable
public final class ModuleClaimOperator extends Module {
    private static final String REPLY_PROMPT = "A key has been generated and logged to the console. Paste it in chat to claim operator. (30s timeout)";
    private static final String REPLY_VALID = "Key valid. Hello, guardian.";
    private static final String REPLY_TIMEOUT = "Operator claim timed out.";

    @SuppressWarnings("CanBeFinal") private static Random rng;

    @Autowired private ApplicationMetaRepository amRepo;

    static {
        try {
            rng = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            Logger.error(e);
        }
    }

    @ReflectiveAccess
    public ModuleClaimOperator() {
        super(new ModuleInfo.Builder(ModuleClaimOperator.class)
                .withName("claimoperator")
                .withDescription("Claim operator for this bot instance"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        // Generate key & fetch app instance
        String key = generateRandomString();

        // Log key and prompt user for it
        ctx.replyBlocking(REPLY_PROMPT);
        Logger.info("Your key: " + key);

        // Listen for & validate key
        // If valid, the new operator's ID gets saved to ghost.properties and the config values are updated
        ctx.getClient().getEventDispatcher().on(MessageCreateEvent.class)
                .filter(event -> event.getMember().isPresent())
                .filter(event -> ctx.getInvoker().getId().equals(event.getMember().orElseThrow().getId()))
                .filter(event -> ctx.getChannel().getId().equals(event.getMessage().getChannelId()))
                .take(1)
                .doOnNext(event -> {
                    if (event.getMessage().getContent().orElse("").equals(key)) {
                        ctx.replyBlocking(REPLY_VALID);
                        Snowflake id = ctx.getInvoker().getId();
                        amRepo.save(new ApplicationMeta(id.asLong()));
                        Logger.info("New operator! ID: " + id.asString());
                    }
                })
                .timeout(Duration.of(30L, ChronoUnit.SECONDS), s -> ctx.replyBlocking(REPLY_TIMEOUT))
                .doOnComplete(References::reloadConfig)
                .subscribe();
    }

    private String generateRandomString() {
        StringBuilder ret = new StringBuilder();
        String[] validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".split("");
        while (ret.length() < 80) {
            ret.append(validChars[rng.nextInt(validChars.length)]);
        }
        return ret.toString();
    }
}
