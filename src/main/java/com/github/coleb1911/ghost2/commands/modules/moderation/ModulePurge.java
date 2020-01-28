package com.github.coleb1911.ghost2.commands.modules.moderation;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import reactor.core.publisher.Flux;

import javax.validation.constraints.NotNull;

public final class ModulePurge extends Module {
    @ReflectiveAccess
    public ModulePurge() {
        super(new ModuleInfo.Builder(ModulePurge.class)
                .withName("purge")
                .withDescription("Clear messages from a channel")
                .withPermissions(PermissionSet.of(Permission.MANAGE_MESSAGES))
                .withAliases("prune"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        // Check for arguments
        if (ctx.getArgs().isEmpty()) {
            ctx.replyBlocking("Please specify a number of messages to purge.");
            return;
        }

        // Try to parse number argument
        int count;
        try {
            count = Integer.parseInt(ctx.getArgs().get(0));
        } catch (NumberFormatException e) {
            ctx.replyBlocking(Module.REPLY_ARGUMENT_INVALID);
            return;
        }

        // Remove messages
        MessageChannel channel = ctx.getChannel();
        Flux.just(ctx.getMessage().getId())
                .flatMap(channel::getMessagesBefore)
                .take(count)
                .flatMap(Message::delete)
                .subscribe();
        ctx.getMessage().delete().subscribe();
    }
}