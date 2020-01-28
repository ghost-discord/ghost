package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.music.MusicService;
import com.github.coleb1911.ghost2.music.MusicUtils;

import javax.validation.constraints.NotNull;

public final class ModuleSkip extends Module {
    @ReflectiveAccess
    public ModuleSkip() {
        super(new ModuleInfo.Builder(ModuleSkip.class)
                .withName("skip")
                .withDescription("Skip the current track"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        MusicUtils.fetchMusicService(ctx)
                .flatMap(MusicService::next)
                .doOnSuccess(ignore -> ctx.getMessage().addReaction(REACT_OK).subscribe())
                .doOnError(ignore -> ctx.getMessage().addReaction(REACT_WARNING).subscribe())
                .subscribe();
    }
}
