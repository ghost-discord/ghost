package com.github.coleb1911.ghost2.commands;

import com.github.coleb1911.ghost2.Ghost2Application;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.CommandType;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.commands.modules.operator.ModuleClaimOperator;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.ApplicationMetaRepository;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

/**
 * Processes {@link MessageCreateEvent}s from the main application class, and calls {@link Module#invoke invoke}
 * when a valid command is invoked in chat.
 */
@Component
@Configurable
public final class CommandDispatcher {
    private final Scheduler commandScheduler = Schedulers.newBoundedElastic(
            Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
            Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
            "CommandDispatcher"
    );
    private final GuildMetaRepository guildRepo;
    private final ApplicationMetaRepository amRepo;
    private final CommandRegistry registry;

    /**
     * Construct a new CommandDispatcher.
     */
    @Autowired
    @ReflectiveAccess
    public CommandDispatcher(GuildMetaRepository guildRepo, ApplicationMetaRepository amRepo, CommandRegistry registry) {
        this.guildRepo = guildRepo;
        this.amRepo = amRepo;
        this.registry = registry;
    }

    /**
     * Processes {@link MessageCreateEvent}s and {@link Module#invoke invoke}s commands.
     * <p>
     * Should <b>NOT</b> be called by anything other than {@link Ghost2Application}.
     *
     * @param event Event to process
     */
    public void onMessageEvent(MessageCreateEvent event) {
        // Build command context
        final CommandContext context = new CommandContext(event);

        // Isolate command name
        Optional<String> commandNameOpt = isolateCommand(context, event);
        if (commandNameOpt.isEmpty()) return;

        // Get Module instance
        Optional<Module> moduleOpt = commandNameOpt.flatMap(registry::getCommandInstance);
        if (moduleOpt.isEmpty()) return;

        // Check permissions
        if (!checkPerms(moduleOpt.get(), context)) return;

        // Finally kick off command thread if all checks are passed
        Mono<?> invokeMono = Mono.fromRunnable(() -> moduleOpt.ifPresent(m -> m.invoke(context)))
                .publishOn(commandScheduler);
        if (moduleOpt.get().getInfo().shouldType()) {
            context.getChannel()
                    .typeUntil(invokeMono)
                    .subscribe();
        } else invokeMono.subscribe();
    }

    private Optional<String> isolateCommand(CommandContext ctx, MessageCreateEvent event) {
        // Fetch prefix from database
        // GuildMeta is more than likely not null. We wouldn't have received the event unless a race condition occurred.
        // We still null-check to be safe and get rid of the warning.
        Optional<Snowflake> guildIdOptional = event.getGuildId();
        if (guildIdOptional.isEmpty()) return Optional.empty();

        final Optional<GuildMeta> meta = guildRepo.findById(event.getGuildId().orElseThrow());
        if (meta.isEmpty()) return Optional.empty();
        String prefix = meta.map(GuildMeta::getPrefix).get();

        // Split on whitespace and retrieve first token
        String firstToken = event.getMessage().getContent()
                .map(msg -> msg.split("\\p{javaSpaceChar}"))
                .map(components -> components[0])
                .orElse("");

        // Isolate command name
        String commandName = null;
        if (firstToken.indexOf(prefix) == 0) {
            commandName = firstToken.replace(prefix, "");
        } else if (firstToken.equals(ctx.getSelf().getMention())) {
            commandName = ctx.getArgs().remove(0);
        }
        return Optional.ofNullable(commandName);
    }

    private boolean checkPerms(final Module module, final CommandContext ctx) {
        // Check user's permissions
        PermissionSet invokerPerms = ctx.getInvoker().getBasePermissions().block();
        if (null == invokerPerms) {
            ctx.replyBlocking(Module.REPLY_GENERAL_ERROR);
            return false;
        }
        if (!invokerPerms.contains(Permission.ADMINISTRATOR)) {
            for (Permission required : module.getInfo().getUserPermissions()) {
                if (!invokerPerms.contains(required)) {
                    ctx.replyBlocking(Module.REPLY_INSUFFICIENT_PERMISSIONS_USER);
                    return false;
                }
            }
        }

        // Check user's ID if command is an operator command
        // An exception is made for ModuleClaimOperator
        if (!(module instanceof ModuleClaimOperator) &&
                (module.getInfo().getType() == CommandType.OPERATOR) &&
                (ctx.getInvoker().getId().asLong() != amRepo.getOperatorId())) {

            ctx.replyBlocking(Module.REPLY_INSUFFICIENT_PERMISSIONS_USER);
            return false;
        }

        // Check bot's permissions
        PermissionSet botPerms = ctx.getSelf().getBasePermissions().block();
        if (null == botPerms) {
            ctx.replyBlocking(Module.REPLY_GENERAL_ERROR);
            return false;
        }
        if (!botPerms.contains(Permission.ADMINISTRATOR)) {
            for (Permission required : module.getInfo().getBotPermissions()) {
                if (!botPerms.contains(required)) {
                    ctx.replyBlocking(Module.REPLY_INSUFFICIENT_PERMISSIONS_BOT);
                    return false;
                }
            }
        }

        return true;
    }

    public CommandRegistry getRegistry() {
        return registry;
    }

    public void shutdown() {
        commandScheduler.dispose();
    }
}