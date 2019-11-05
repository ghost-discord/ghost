package com.github.coleb1911.ghost2.music.youtube;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.pmw.tinylog.Logger;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URISyntaxException;

public class YoutubeScrapeSearchProvider implements YoutubeSearchProvider {
    private static final String BASE_URI = "https://youtube.com";
    private static final String SEARCH_URI = BASE_URI + "/results";

    @Override
    public YoutubeSearchResult search(String term) {
        try {
            String query = new URIBuilder(BASE_URI)
                    .setPath("results")
                    .addParameter("search_query", term)
                    .build()
                    .toString();

            return Flux.just(Jsoup.connect(query).get())
                    .flatMapIterable(e -> e.getElementsByClass("yt-lockup-content"))
                    .filter(res -> res.getElementsByClass("yt-lockup-playlist-items").isEmpty())
                    .map(element -> {
                        Element resultAnchor = element.getElementsByClass("yt-lockup-title").first().getElementsByTag("a").first();
                        String title = resultAnchor.text();
                        String uri = BASE_URI + resultAnchor.attr("href");
                        return new YoutubeSearchResult(title, uri);
                    })
                    .filter(result -> !StringUtils.containsAny(result.uri, "user", "pagead"))
                    .take(1)
                    .blockFirst();
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
        }
        return null;
    }
}
