package com.github.coleb1911.ghost2.commands.modules.info;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import discord4j.core.object.util.Snowflake;

public class ModuleInvite extends Module {
    public ModuleInvite() {
        super(new ModuleInfo.Builder(ModuleInvite.class)
                .withName("invite")
                .withDescription("Generates invite link for the bot"));
    }

    @Override
    public void invoke(CommandContext ctx) {
        Snowflake clientId = ctx.getClient().getApplicationInfo().block().getId();
        ctx.reply(String.format("https://discordapp.com/oauth2/authorize?client_id=%d&scope=bot&permissions=3474432", clientId.asLong()));
    }
}
