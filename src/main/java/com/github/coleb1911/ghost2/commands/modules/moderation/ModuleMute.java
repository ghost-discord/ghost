package com.github.coleb1911.ghost2.commands.modules.moderation;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

public final class ModuleMute extends Module {
    public ModuleMute() {
        super(new ModuleInfo.Builder(ModuleMute.class)
                .withName("mute")
                .withDescription("Mute a desired user.")
                .withBotPermissions(PermissionSet.of(Permission.MUTE_MEMBERS)));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        if(ctx.getArgs().isEmpty()) {
            ctx.replyBlocking("Please specify a user.");
            return;
        }

        String targetName = ctx.getArgs().get(0);
        ctx.getGuild().getMembers()
                .filter(member -> targetName.equals(member.getDisplayName()))
                .map(member -> member.edit(spec -> {
                    spec.setMute(true);
                    ctx.replyBlocking(member.getDisplayName() + " is now muted.");
                }).subscribe())
                .hasElements()
                .flatMap(aBoolean -> {
                    if (!aBoolean) ctx.replyBlocking("User not found.");
                    return Mono.just(aBoolean);
                })
                .subscribe();
    }
}
