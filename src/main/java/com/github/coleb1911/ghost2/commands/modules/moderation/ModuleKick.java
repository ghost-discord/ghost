package com.github.coleb1911.ghost2.commands.modules.moderation;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.rest.http.client.ClientException;

import javax.validation.constraints.NotNull;

public final class ModuleKick extends Module {
    @ReflectiveAccess
    public ModuleKick() {
        super(new ModuleInfo.Builder(ModuleKick.class)
                .withName("kick")
                .withDescription("Kick a user")
                .withPermissions(PermissionSet.of(Permission.KICK_MEMBERS)));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        // Check for args
        if (ctx.getArgs().isEmpty()) {
            ctx.replyBlocking("Please specify a user.");
            return;
        }

        // Use mention if available, otherwise search for user
        if (!ctx.getUserMentions().isEmpty()) {
            User target = ctx.getUserMentions().get(0);
            target.asMember(ctx.getGuild().getId())
                    .doOnError(e -> {
                        if (e instanceof ClientException && ((ClientException) e).getStatus().code() == 50013)
                            ctx.replyBlocking("I don't have permission to kick that user.");
                    })
                    .subscribe(m -> m.kick().subscribe());
        } else {
            String targetName = ctx.getArgs().get(0);
            ctx.getGuild().getMembers()
                    .doOnError(e -> {
                        if (e instanceof ClientException && ((ClientException) e).getStatus().code() == 50013)
                            ctx.replyBlocking("I don't have permission to kick that user.");
                    })
                    .take(1)
                    .filter(member -> targetName.equals(member.getDisplayName()))
                    .map(m -> m.kick().subscribe())
                    .collectList()
                    .subscribe(monos -> {
                        if (monos.isEmpty())
                            ctx.replyBlocking("No user found with that name.");
                    });
        }
    }
}
