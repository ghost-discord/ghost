package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.music.MusicUtils;
import com.github.coleb1911.ghost2.music.youtube.YoutubeScrapeSearchProvider;
import com.github.coleb1911.ghost2.music.youtube.YoutubeSearchResult;

import javax.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public final class ModulePlay extends Module {

    @ReflectiveAccess
    public ModulePlay() {
        super(new ModuleInfo.Builder(ModulePlay.class)
                .withName("play")
                .withDescription("Play or queue a track.")
                .withAliases("queueadd", "qa")
                .showTypingIndicator());
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        if (ctx.getArgs().isEmpty()) {
            ctx.replyBlocking("Please provide a link to a valid track.");
            return;
        }

        final String arg = ctx.getArgs().get(0);
        final AtomicReference<String> source = new AtomicReference<>();
        final AtomicReference<String> title = new AtomicReference<>();
        if (isUrlValid(arg)) {
            source.set(arg);
        } else {
            String term = String.join(" ", ctx.getArgs());
            YoutubeSearchResult result = new YoutubeScrapeSearchProvider().search(term);
            source.set(result.uri);
            title.set(result.title);
        }

        MusicUtils.fetchMusicService(ctx)
                .flatMap(service -> service.loadTrack(source.get()))
                .subscribe(result -> {
                    if (title.get() != null) ctx.replyBlocking("Queued **" + title + "**.");
                    else ctx.replyBlocking(result.message);
                });
    }

    private boolean isUrlValid(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}