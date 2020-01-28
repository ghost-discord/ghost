package com.github.coleb1911.ghost2.commands.modules.music;

import com.github.coleb1911.ghost2.References;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.music.MusicUtils;
import com.github.coleb1911.ghost2.music.TrackAddResult;
import com.github.coleb1911.ghost2.music.youtube.YoutubeAPISearchProvider;
import com.github.coleb1911.ghost2.music.youtube.YoutubeScrapeSearchProvider;
import com.github.coleb1911.ghost2.music.youtube.YoutubeSearchProvider;
import com.github.coleb1911.ghost2.music.youtube.YoutubeSearchResult;

import javax.validation.constraints.NotNull;
import java.util.function.Consumer;

public final class ModulePlay extends Module {
    @ReflectiveAccess
    public ModulePlay() {
        super(new ModuleInfo.Builder(ModulePlay.class)
                .withName("play")
                .withDescription("Play or queue a track")
                .withAliases("queueadd", "qa")
                .showTypingIndicator());
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        if (ctx.getArgs().isEmpty()) {
            ctx.replyBlocking("Please provide a link to a valid track.");
            return;
        }

        // Handle special case for YouTube playlists
        final String arg0 = ctx.getArgs().get(0);
        if (arg0.contains("list=")) {
            loadTrack(ctx, arg0, res -> ctx.replyBlocking(res.message));
            return;
        }

        // Join arguments for search query
        final String query = String.join(" ", ctx.getArgs());

        // Check if Youtube Data API key is set, fetch appropriate search provider
        final String apiKey = References.getConfig().youtubeApiKey();
        final YoutubeSearchProvider provider;
        if (apiKey != null) provider = new YoutubeAPISearchProvider(apiKey);
        else provider = new YoutubeScrapeSearchProvider();

        // Search and load result
        final YoutubeSearchResult searchResult = provider.search(query);
        if (searchResult == null) {
            ctx.replyBlocking("No results found for \"" + query + "\"");
            return;
        }
        loadTrack(ctx, searchResult.getUri(), r -> ctx.replyEmbedBlocking(searchResult.populateEmbed()));
    }

    private void loadTrack(CommandContext ctx, String uri, Consumer<? super TrackAddResult> onSuccess) {
        MusicUtils.fetchMusicService(ctx)
                .flatMap(service -> service.loadTrack(uri))
                .filter(result -> {
                    if (result == TrackAddResult.FAILED) {
                        ctx.replyBlocking(result.message);
                        return false;
                    }
                    return true;
                })
                .subscribe(onSuccess);
    }
}