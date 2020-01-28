package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.music.MusicService;
import com.github.coleb1911.ghost2.music.MusicUtils;

import javax.validation.constraints.NotNull;

public final class ModulePlaying extends Module {
    @ReflectiveAccess
    public ModulePlaying() {
        super(new ModuleInfo.Builder(ModulePlaying.class)
                .withName("playing")
                .withDescription("Get the currently playing track")
                .withAliases("nowplaying", "np"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        MusicUtils.fetchMusicService(ctx)
                .flatMap(MusicService::getCurrentTrack)
                .subscribe(track -> ctx.replyBlocking("Currently playing **" +
                        track.getInfo().title +
                        "** by **" +
                        track.getInfo().author +
                        "**."));
    }
}
