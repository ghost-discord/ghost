package com.github.coleb1911.ghost2.music.youtube;

public class YoutubeSearchResult {
    public final boolean isRich;
    public final String title;
    public final String uri;
    public final String thumbnailUri;
    public final String description;

    public YoutubeSearchResult(String title, String uri) {
        this.isRich = false;
        this.title = title;
        this.uri = uri;
        this.thumbnailUri = null;
        this.description = null;
    }

    public YoutubeSearchResult(String title, String uri, String thumbnailUri, String description) {
        this.isRich = true;
        this.title = title;
        this.uri = uri;
        this.thumbnailUri = thumbnailUri;
        this.description = description;
    }
}