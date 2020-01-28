package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.music.MusicServiceManager;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

public final class ModuleLeave extends Module {
    @ReflectiveAccess
    public ModuleLeave() {
        super(new ModuleInfo.Builder(ModuleLeave.class)
                .withName("leave")
                .withDescription("Make ghost2 stop music playback and leave")
                .withAliases("stop", "stopmusic"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        if (!MusicServiceManager.serviceExists(ctx.getGuild().getId())) {
            ctx.replyBlocking("I'm not playing music right now.");
            return;
        }

        Mono.just(ctx.getGuild().getId())
                .flatMap(MusicServiceManager::forceCleanup)
                .doOnSuccess(ignore -> ctx.getMessage().addReaction(REACT_OK).subscribe())
                .doOnError(ignore -> ctx.getMessage().addReaction(REACT_WARNING).subscribe())
                .subscribe();
    }
}