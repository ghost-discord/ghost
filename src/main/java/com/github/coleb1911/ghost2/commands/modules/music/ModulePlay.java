package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.music.MusicUtils;
import com.github.coleb1911.ghost2.music.TrackAddResult;
import com.github.coleb1911.ghost2.music.youtube.YoutubeScrapeSearchProvider;
import com.github.coleb1911.ghost2.music.youtube.YoutubeSearchResult;

import javax.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URL;

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
        final YoutubeSearchResult searchResult;
        if (isUrlValid(arg)) {
            searchResult = new YoutubeSearchResult(arg, arg);
        } else {
            String term = String.join(" ", ctx.getArgs());
            searchResult = new YoutubeScrapeSearchProvider().search(term);
        }

        MusicUtils.fetchMusicService(ctx)
                .flatMap(service -> service.loadTrack(searchResult.uri))
                .subscribe(loadResult -> {
                    if (loadResult == TrackAddResult.FAILED) ctx.replyBlocking(loadResult.message);
                    else if (searchResult != null) ctx.replyEmbedBlocking(spec -> {
                        spec.setTitle(searchResult.title);
                        spec.setUrl(searchResult.uri);
                        if (searchResult.isRich) {
                            spec.setDescription(searchResult.description);
                            spec.setThumbnail(searchResult.thumbnailUri);
                        } else {
                            spec.setDescription("Queued track.");
                            spec.setThumbnail(getThumbnailUri(searchResult.uri));
                        }
                    });
                    else ctx.replyBlocking(loadResult.message);
                });
    }

    private boolean isUrlValid(String urlString) {
        try {
            new URL(urlString);
            return urlString.matches(".+v=.+|.+list=.+");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private String getThumbnailUri(String urlString) {
        final String id = urlString.split("v=|list=")[1];
        return "https://img.youtube.com/vi/" + id + "/0.jpg";
    }
}