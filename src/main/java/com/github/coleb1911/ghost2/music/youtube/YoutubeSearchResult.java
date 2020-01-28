package com.github.coleb1911.ghost2.music.youtube;

import discord4j.core.spec.EmbedCreateSpec;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class YoutubeSearchResult {
    private final String title;
    private final String uri;
    private final String thumbnailUri;
    private final String description;
    private final String channelName;
    private final String channelUri;
    private final Instant uploadDate;

    public YoutubeSearchResult(String title, String uri, String thumbnailUri, String description) {
        this.title = title;
        this.uri = uri;
        this.thumbnailUri = thumbnailUri;
        this.description = description;
        this.channelName = null;
        this.channelUri = null;
        this.uploadDate = null;
    }

    public YoutubeSearchResult(String title, String uri, String thumbnailUri, String description,
                               String channelName, String channelUri, Instant uploadDate) {
        this.title = title;
        this.uri = uri;
        this.thumbnailUri = thumbnailUri;
        this.description = description;
        this.channelName = channelName;
        this.channelUri = channelUri;
        this.uploadDate = uploadDate;
    }

    public Consumer<EmbedCreateSpec> populateEmbed() {
        return spec -> {
            spec.setTitle(title);
            spec.setUrl(uri);
            spec.setThumbnail(thumbnailUri);
            spec.setDescription(description);
            if (Stream.of(channelName, channelUri, uploadDate).allMatch(Objects::nonNull)) {
                spec.setAuthor(channelName, channelUri, null);
                spec.setTimestamp(uploadDate);
            }
        };
    }

    public String getUri() {
        return uri;
    }
}