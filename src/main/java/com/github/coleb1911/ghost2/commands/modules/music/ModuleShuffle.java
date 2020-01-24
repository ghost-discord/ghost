package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.music.MusicService;
import com.github.coleb1911.ghost2.music.MusicUtils;

import javax.validation.constraints.NotNull;

public final class ModuleShuffle extends Module {
    public ModuleShuffle() {
        super(new ModuleInfo.Builder(ModuleShuffle.class)
                .withName("shuffle")
                .withDescription("Shuffle the tracks in the queue.")
                .withAliases("queueshuffle", "qs")
                .showTypingIndicator());
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        MusicUtils.fetchMusicService(ctx)
                .flatMap(MusicService::shuffle)
                .doOnSuccess(ignore -> ctx.getMessage().addReaction(REACT_OK).subscribe())
                .doOnError(ignore -> ctx.getMessage().addReaction(REACT_WARNING).subscribe())
                .subscribe();
    }
}
