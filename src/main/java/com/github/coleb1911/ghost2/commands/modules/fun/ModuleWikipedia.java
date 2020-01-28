package com.github.coleb1911.ghost2.commands.modules.fun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.utility.RestUtils;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import org.jsoup.Jsoup;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;

/**
 * Module for searching wikipedia and displaying the most recent result
 */
// https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=Nelson%20Mandela&utf8=&format=json
@Service
public final class ModuleWikipedia extends Module {
    private final URI baseURI;
    private final RestTemplate restTemplate;

    public ModuleWikipedia() {
        this("https://en.wikipedia.org/", RestUtils.defaultRestTemplate());
    }


    public ModuleWikipedia(String baseUrl, RestTemplate restTemplate) {
        super(new ModuleInfo.Builder(ModuleWikipedia.class)
                .withName("wikipedia")
                .withAliases("wiki", "w")
                .withDescription("Look up an article on Wikipedia")
                .withBotPermissions(PermissionSet.of(Permission.EMBED_LINKS)));
        this.baseURI = URI.create(baseUrl);
        this.restTemplate = restTemplate;
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        String searchString = String.join(" ", ctx.getArgs());
        String urlSearchString = URLEncoder.encode(searchString, Charset.defaultCharset());
        String response;
        URI resultQuery = baseURI.resolve("/w/api.php?action=query&list=search&utf8=&format=json&srsearch=" + urlSearchString);
        try {
            ResponseEntity<WikipediaSearchResults> results = restTemplate.getForEntity(resultQuery, WikipediaSearchResults.class);
            SearchInfo searchInfo = results.getBody().getQueryResult().getSearchInfo();
            List<SearchResult> searchResults = results.getBody().getQueryResult().getSearchResults();
            if (!searchResults.isEmpty()) {
                SearchResult topResult = searchResults.get(0);
                ctx.getChannel().createMessage(message -> message.setEmbed(embedCreateSpec -> {
                    embedCreateSpec.setTitle(topResult.title);
                    embedCreateSpec.setThumbnail("https://en.wikipedia.org/static/images/project-logos/enwiki-2x.png");
                    embedCreateSpec.setDescription(Jsoup.parse(topResult.snippet).text());
                })).block();
            } else {
                if (searchInfo.hasSuggestions()) {
                    ctx.replyBlocking("Did you mean \"" + searchInfo.getSuggestion() + "\"?");
                } else {
                    ctx.replyBlocking("Couldn't find anything about \"" + searchString + "\" on wikipedia.");
                }
            }
        } catch (RestClientException ex) {
            response = "Error: " + ex.toString();
            ctx.replyBlocking(response);
        }
    }

    private static class SearchInfo {
        @JsonProperty(value = "totalHits")
        private int totalHits;

        @JsonProperty(value = "suggestion")
        private String suggestion;

        @JsonProperty(value = "suggestionsnippet")
        private String suggestionSnippet;

        public String getSuggestion() {
            return suggestion;
        }

        public boolean hasSuggestions() {
            return suggestion != null && !suggestion.isBlank();
        }

        public boolean hasResults() {
            return totalHits >= 1;
        }
    }

    private static class SearchResult {
        @JsonProperty(value = "ns")
        private int ns;

        @JsonProperty(value = "title")
        private String title;

        @JsonProperty(value = "pageid")
        private int pageId;

        @JsonProperty(value = "size")
        private int size;

        @JsonProperty(value = "wordcount")
        private int wordCount;

        @JsonProperty(value = "snippet")
        private String snippet;

        @JsonProperty(value = "timestamp")
        private Instant timestamp;

        public String getSnippet() {
            return snippet;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QueryResult {
        @JsonProperty(value = "searchinfo")
        private SearchInfo searchInfo;

        @JsonProperty(value = "search")
        private List<SearchResult> searchResults;

        public List<SearchResult> getSearchResults() {
            return searchResults;
        }

        public SearchInfo getSearchInfo() {
            return searchInfo;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ContinueObject {
        @JsonProperty(value = "sroffset")
        private int srOffset;

        @JsonProperty(value = "continue")
        private String continueString;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WikipediaSearchResults {
        @JsonProperty(value = "batchcomplete")
        private String batchComplete;

        @JsonProperty(value = "continue")
        private ContinueObject continueObject;

        @JsonProperty(value = "query")
        private QueryResult queryResult;

        public QueryResult getQueryResult() {
            return queryResult;
        }
    }
}
