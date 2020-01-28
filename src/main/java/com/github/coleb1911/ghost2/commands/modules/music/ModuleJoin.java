package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.music.MusicUtils;

import javax.validation.constraints.NotNull;

public final class ModuleJoin extends Module {
    @ReflectiveAccess
    public ModuleJoin() {
        super(new ModuleInfo.Builder(ModuleJoin.class)
                .withName("join")
                .withDescription("Make ghost2 join your current voice channel"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        MusicUtils.fetchMusicService(ctx)
                .doOnNext(ignore -> ctx.getMessage().addReaction(REACT_OK).subscribe())
                .doOnError(ignore -> ctx.getMessage().addReaction(REACT_WARNING).subscribe())
                .subscribe();
    }
}
