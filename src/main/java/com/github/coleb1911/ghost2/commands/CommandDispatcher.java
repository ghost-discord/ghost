package com.github.coleb1911.ghost2.commands;

import com.github.coleb1911.ghost2.commands.meta.Command;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.CommandType;
import com.github.coleb1911.ghost2.database.entities.GuildMeta;
import com.github.coleb1911.ghost2.database.repos.GuildMetaRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import org.pmw.tinylog.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Configurable
public class CommandDispatcher {
    private static final long OPERATOR_ID = 195635151005417472L;

    @Autowired
    private GuildMetaRepository guildRepo;
    private Set<Class<? extends Command>> modules;
    private CommandRegistry registry;

    public CommandDispatcher() {
        try {
            registry = new CommandRegistry();
        } catch (ReflectiveOperationException e) {
            Logger.error(e);
        }
    }

    public void onMessageEvent(MessageCreateEvent ev) {
        // Build command context
        final CommandContext ctx = new CommandContext(ev);

        // Fetch prefix from database
        // GuildMeta shouldn't be null, but in a few rare edge cases it can be
        GuildMeta meta = guildRepo.findById(ctx.getGuild().getId().asLong()).orElse(null);
        if (null == meta) return;
        String prefix = meta.getPrefix();

        // Check for prefix, strip it if it exists
        String trigger = ctx.getTrigger();
        String commandName;
        if (trigger.indexOf(prefix) == 0)
            commandName = trigger.replace(prefix, "");
        else return;

        // Get command instance
        final Command command;
        try {
            command = registry.getCommandInstance(commandName);
        } catch (ReflectiveOperationException e) {
            Logger.error(e);
            return;
        }

        // Check operator permissions
        if ((command.getType() == CommandType.OPERATOR) && (ctx.getInvoker().getId().asLong() != OPERATOR_ID)) {
            ctx.reply(Command.REPLY_INSUFFICIENT_PERMISSIONS);
            return;
        }

        // Check role permissions
        PermissionSet invokerPerms = ctx.getInvoker().getHighestRole()
                .map(Role::getPermissions)
                .block();
        for (Permission perm : command.getRequiredPermissions()) {
            assert invokerPerms != null;
            if (!invokerPerms.contains(perm)) {
                ctx.reply(Command.REPLY_INSUFFICIENT_PERMISSIONS);
                return;
            }
        }

        // Kick off command thread
        new Thread(() -> command.invoke(ctx)).run();
    }
}
