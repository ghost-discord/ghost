package com.github.coleb1911.ghost2.commands.modules.moderation;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import discord4j.core.object.entity.Member;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import reactor.core.publisher.Mono;

import java.util.List;

public class ModuleKick extends Module {
    public ModuleKick() {
        super(new ModuleInfo.Builder(ModuleKick.class)
                .withName("kick")
                .withDescription("Kick a desired user")
                .withBotPermissions(PermissionSet.of(Permission.KICK_MEMBERS)));
    }

    @Override
    public void invoke(CommandContext ctx) {
        //Check for args
        try{
            ctx.getArgs().get(0);
        } catch(IndexOutOfBoundsException e) {
            ctx.reply("Please specify a user.");
            return;
        }

        String targetName = ctx.getArgs().get(0);
        ctx.getGuild().getMembers()
                .filter(member -> targetName.equals(member.getDisplayName()))
                .map(member -> member.kick().subscribe())
                .hasElements()
                .flatMap(aBoolean -> {
                    if(!aBoolean) ctx.reply("User not found.");
                    return Mono.just(aBoolean);
                })
                .subscribe();
    }
}
