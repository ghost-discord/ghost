package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.music.MusicUtils;

import javax.validation.constraints.NotNull;

public final class ModulePlay extends Module {

    @ReflectiveAccess
    public ModulePlay() {
        super(new ModuleInfo.Builder(ModulePlay.class)
                .withName("play")
                .withDescription("Play or queue a track.")
                .withAliases("queueadd", "qa"));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        if (ctx.getArgs().size() < 1) {
            ctx.reply("Please provide a link to a valid track.");
            return;
        }

        MusicUtils.fetchMusicService(ctx)
                .flatMap(service -> service.loadTrack(ctx.getArgs().get(0)))
                .subscribe(result -> ctx.reply(result.message));
    }
}