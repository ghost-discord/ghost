package com.github.coleb1911.ghost2.commands.modules.info;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;

import javax.validation.constraints.NotNull;

public final class ModuleInvite extends Module {
    private static final String BASE_URL = "https://discordapp.com/oauth2/authorize?scope=bot&permissions=3214336&client_id=";

    @ReflectiveAccess
    public ModuleInvite() {
        super(new ModuleInfo.Builder(ModuleInvite.class)
                .withName("invite")
                .withDescription("Generates invite link for the bot")
                .showTypingIndicator());
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull final CommandContext ctx) {
        ctx.getClient().getSelfId()
                .ifPresent(id -> ctx.replyBlocking(BASE_URL + id.asLong()));
    }
}