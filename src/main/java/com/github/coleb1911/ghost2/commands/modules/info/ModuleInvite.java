package com.github.coleb1911.ghost2.commands.modules.info;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.util.Snowflake;

import javax.validation.constraints.NotNull;
import java.util.Objects;

public final class ModuleInvite extends Module {
    @ReflectiveAccess
    public ModuleInvite() {
        super(new ModuleInfo.Builder(ModuleInvite.class)
                .withName("invite")
                .withDescription("Generates invite link for the bot"));
    }

    @Override
    public void invoke(@NotNull final CommandContext ctx) {
        Snowflake clientId = Objects.requireNonNull(ctx.getClient().getApplicationInfo().block()).getId();
        ctx.reply("https://discordapp.com/oauth2/authorize?client_id=" + clientId.asLong() + "&scope=bot&permissions=3214336");
    }
}