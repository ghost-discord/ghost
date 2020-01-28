package com.github.coleb1911.ghost2.music.youtube;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.utility.RestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

/**
 * A fully featured YoutubeSearchProvider that searches using the official API.
 */
public class YoutubeAPISearchProvider implements YoutubeSearchProvider {
    private static final URI API_URI = URI.create("https://www.googleapis.com/youtube/v3/search");
    private static final URI SITE_URI = URI.create("https://youtube.com");

    private final String apiKey;
    private final RestTemplate template = RestUtils.defaultRestTemplate();

    public YoutubeAPISearchProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public YoutubeSearchResult search(String query) {
        try {
            URI requestUri = new URIBuilder(API_URI)
                    .addParameter("key", apiKey)
                    .addParameter("part", "snippet")
                    .addParameter("type", "video")
                    .addParameter("q", query)
                    .build();

            // Send request
            ResponseEntity<YoutubeSearchResponse> response = template.getForEntity(requestUri, YoutubeSearchResponse.class);
            if (response.getBody().items.length < 1) return null;
            YoutubeSearchResource resource = response.getBody().items[0];

            // Collect result data
            String videoUri = SITE_URI.resolve("/watch?v=" + resource.getVideoId()).toString();
            String channelUri = SITE_URI.resolve("/channel/" + resource.getChannelId()).toString();
            return new YoutubeSearchResult(resource.getTitle(), videoUri, resource.getDefaultThumbnail(), resource.getDescription(),
                    resource.getChannelName(), channelUri, resource.getUploadTime());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class YoutubeSearchResource {
        @JsonProperty("kind")
        private String kind;

        @JsonProperty("id")
        private ID id;

        @JsonProperty("snippet")
        private Snippet snippet;

        private static class ID {
            @JsonProperty("kind")
            private String kind;

            @JsonProperty("videoId")
            private String videoId;
        }

        private static class Snippet {
            @JsonProperty("publishedAt")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            private Instant publishedAt;

            @JsonProperty("channelId")
            private String channelId;

            @JsonProperty("title")
            private String title;

            @JsonProperty("description")
            private String description;

            @JsonProperty("thumbnails")
            private Thumbnails thumbnails;

            @JsonProperty("channelTitle")
            private String channelTitle;
        }

        private static class Thumbnails {
            @JsonProperty("default")
            private Thumbnail low;

            @JsonProperty("medium")
            private Thumbnail medium;

            @JsonProperty("high")
            private Thumbnail high;
        }

        private static class Thumbnail {
            @JsonProperty("url")
            private String url;

            @JsonProperty("width")
            private int width;

            @JsonProperty("height")
            private int height;
        }

        private String getTitle() {
            return snippet.title;
        }

        private String getVideoId() {
            return id.videoId;
        }

        private String getDefaultThumbnail() {
            return snippet.thumbnails.low.url;
        }

        private String getDescription() {
            return snippet.description;
        }

        private String getChannelName() {
            return snippet.channelTitle;
        }

        private String getChannelId() {
            return snippet.channelId;
        }

        private Instant getUploadTime() {
            return snippet.publishedAt;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class YoutubeSearchResponse {
        @JsonProperty("items")
        private YoutubeSearchResource[] items;
    }
}
