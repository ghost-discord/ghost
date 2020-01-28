package com.github.coleb1911.ghost2.music.youtube;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.pmw.tinylog.Logger;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A cheap, minimal {@link YoutubeSearchProvider} that finds results by scraping the site.
 */
public class YoutubeScrapeSearchProvider implements YoutubeSearchProvider {
    private static final URI BASE_URI = URI.create("https://youtube.com/");

    @Override
    public YoutubeSearchResult search(String query) {
        try {
            String requestUri = new URIBuilder(BASE_URI)
                    .setPath("results")
                    .addParameter("search_query", query)
                    .build()
                    .toString();

            return Flux.just(Jsoup.connect(requestUri).get())
                    .flatMapIterable(e -> e.getElementsByClass("yt-lockup-content"))
                    .filter(res -> res.getElementsByClass("yt-lockup-playlist-items").isEmpty())
                    .map(root -> {
                        Element resultAnchor = root.getElementsByClass("yt-lockup-title").first().getElementsByTag("a").first();
                        Element description = root.getElementsByClass("yt-lockup-description").first();
                        String uri = BASE_URI + resultAnchor.attr("href");
                        return new YoutubeSearchResult(resultAnchor.text(), uri, getThumbnailUri(uri), description.text());
                    })
                    .filter(result -> !StringUtils.containsAny(result.getUri(), "user", "pagead"))
                    .take(1)
                    .blockFirst();
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
        }
        return null;
    }

    private String getThumbnailUri(String urlString) {
        final String id = urlString.split("v=|list=")[1];
        return "https://img.youtube.com/vi/" + id + "/0.jpg";
    }
}
