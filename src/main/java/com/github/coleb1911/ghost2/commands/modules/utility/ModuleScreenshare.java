package com.github.coleb1911.ghost2.commands.modules.utility;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import discord4j.core.object.VoiceState;
import discord4j.core.object.util.Snowflake;

import javax.validation.constraints.NotNull;
import java.util.Optional;

public final class ModuleScreenshare extends Module {
    private static final String REPLY_NO_VOICE_CHANNEL = "You are not in a voice channel. Please join a voice channel to use the `screenshare` command.";

    public ModuleScreenshare() {
        super(new ModuleInfo.Builder(ModuleScreenshare.class)
                .withName("screenshare")
                .withDescription("Generates a screenshare link."));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        // Check if user is connected to a voice channel
        Optional<Snowflake> channelIdOptional = ctx.getInvoker().getVoiceState().map(VoiceState::getChannelId).block();
        if (Optional.empty().equals(channelIdOptional)) {
            ctx.reply(REPLY_NO_VOICE_CHANNEL);
            return;
        }

        ctx.reply("https://discordapp.com/channels/" + ctx.getGuild().getId().asLong() + "/" + channelIdOptional);
    }
}