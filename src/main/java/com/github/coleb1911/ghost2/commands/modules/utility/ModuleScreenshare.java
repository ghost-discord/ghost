package com.github.coleb1911.ghost2.commands.modules.utility;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.VoiceState;

import javax.validation.constraints.NotNull;
import java.util.Optional;

public final class ModuleScreenshare extends Module {
    private static final String REPLY_NO_VOICE_CHANNEL = "You are not in a voice channel. Please join a voice channel to use the `screenshare` command.";

    @ReflectiveAccess
    public ModuleScreenshare() {
        super(new ModuleInfo.Builder(ModuleScreenshare.class)
                .withName("screenshare")
                .withDescription("Generates a screenshare link"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        // Check if user is connected to a voice channel
        ctx.getInvoker().getVoiceState()
                .map(VoiceState::getChannelId)
                .defaultIfEmpty(Optional.empty())
                .subscribe(opt -> {
                    if (opt.isEmpty()) {
                        ctx.replyBlocking(REPLY_NO_VOICE_CHANNEL);
                        return;
                    }

                    ctx.replyBlocking("https://discordapp.com/channels/" +
                            ctx.getGuild().getId().asString() +
                            "/" +
                            opt.get().asString());
                });
    }
}