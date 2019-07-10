package com.github.coleb1911.ghost2.commands.modules.moderation;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;

public class ModulePurge extends Module {
    public ModulePurge() {
        super(new ModuleInfo.Builder(ModulePurge.class)
                .withName("purge")
                .withDescription("Clear messages from a channel")
                .withBotPermissions(PermissionSet.of(Permission.MANAGE_MESSAGES)));
    }

    @Override
    public void invoke(CommandContext ctx) {
        ctx.getMessage().delete().subscribe();

        // Try to parse number argument, default to 10
        int count;
        try {
            count = Integer.parseInt(ctx.getArgs().get(0));
        } catch (NumberFormatException e) {
            ctx.reply(Module.REPLY_ARGUMENT_INVALID);
            return;
        } catch (IndexOutOfBoundsException e) {
            count = 10;
        }

        ctx.getChannel().getMessagesBefore(ctx.getMessage().getId())
                .take(count)
                .map(message -> message.delete().subscribe())
                .retry(5L)
                .blockLast();
    }
}