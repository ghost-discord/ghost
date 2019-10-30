package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.music.MusicService;
import com.github.coleb1911.ghost2.music.MusicUtils;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import javax.validation.constraints.NotNull;

public final class ModuleQueue extends Module {
    public ModuleQueue() {
        super(new ModuleInfo.Builder(ModuleQueue.class)
                .withName("queue")
                .withDescription("Show the current tracks in the queue."));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        MusicUtils.fetchMusicService(ctx)
                .flux()
                .flatMap(MusicService::streamTracks)
                .collectList()
                .subscribe(tracks -> {
                    if (tracks.isEmpty()) {
                        ctx.reply("Queue is empty.");
                        return;
                    }

                    // TODO: Display a multi-page interactive embed here
                    ctx.replyEmbed(spec -> {
                        for (int i = 0; i < tracks.size(); i++) {
                            AudioTrackInfo info = tracks.get(i).getInfo();
                            spec.addField((i + 1) + ". " + info.title, info.author, false);
                        }
                    });
                });
    }
}
