package com.github.coleb1911.ghost2.commands.modules.moderation;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

public final class ModuleBan extends Module {
    @ReflectiveAccess
    public ModuleBan() {
        super(new ModuleInfo.Builder(ModuleBan.class)
                .withName("ban")
                .withDescription("Ban a user")
                .withBotPermissions(PermissionSet.of(Permission.BAN_MEMBERS)));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        if(ctx.getArgs().isEmpty()) {
            ctx.replyBlocking("Please specify a user.");
            return;
        }

        String targetName = ctx.getArgs().get(0);
        ctx.getGuild().getMembers()
                .filter(member -> targetName.equals(member.getDisplayName()))
                .map(member -> member.ban(spec -> {
                    spec.setReason("Banned by " + ctx.getInvoker().getDisplayName());
                    ctx.replyBlocking(member.getDisplayName() + " was banned.");
                }).subscribe())
                .hasElements()
                .flatMap(aBoolean -> {
                    if (!aBoolean) ctx.replyBlocking("User not found.");
                    return Mono.just(aBoolean);
                })
                .subscribe();
    }
}
