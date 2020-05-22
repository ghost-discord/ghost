package com.github.coleb1911.ghost2.commands.modules.fun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.utility.RestUtils;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import org.jsoup.Jsoup;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

public class ModuleUrbanDictionary extends Module {

    private final URI baseURI;
    private final RestTemplate template;

    public ModuleUrbanDictionary() {
        this("https://mashape-community-urban-dictionary.p.rapidapi.com/", RestUtils.defaultRestTemplate());
    }

    public ModuleUrbanDictionary(String baseUrl, RestTemplate restTemplate) {
        super(new ModuleInfo.Builder(ModuleUrbanDictionary.class)
                .withName("urban")
                .withDescription("Search random urban dictionary entry")
                .withBotPermissions(PermissionSet.of(Permission.EMBED_LINKS)));

        this.baseURI = URI.create(baseUrl);
        this.template = restTemplate;
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {

        String searchString = String.join(" ", ctx.getArgs());
        String urlSearchString = URLEncoder.encode(searchString, Charset.defaultCharset());
        URI resultQuery = baseURI.resolve(baseURI.getPath() + "define?term=" + urlSearchString);
        try {
            ResponseEntity<UrbanDictionarySearchResults> results = template.getForEntity(resultQuery, UrbanDictionarySearchResults.class);
            List<SearchResult> searchResults = Objects.requireNonNull(results.getBody()).searchResults;

            if (!searchResults.isEmpty()) {
                SearchResult topResult = searchResults.get(0);
                ctx.getChannel().createMessage(message -> message.setEmbed(embedCreateSpec -> {
                    embedCreateSpec.setTitle(urlSearchString);
                    embedCreateSpec.setAuthor(topResult.author, null, null);
                    embedCreateSpec.setDescription(Jsoup.parse(topResult.definition).text());
                    embedCreateSpec.setUrl(topResult.permalink);
                    embedCreateSpec.setThumbnail("https://img.utdstc.com/icons/urban-dictionary-android.png");
                })).block();
            } else {
                ctx.replyBlocking(String.format("Could not find anything on \"%s\" in Urban Dictionary", urlSearchString));
            }
        } catch (RestClientException e) {
            ctx.replyBlocking("Error: " + e.toString());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SearchResult {

        @JsonProperty(value = "definition")
        private String definition;

        @JsonProperty(value = "permalink")
        private String permalink;

        @JsonProperty(value = "thumbs_up")
        private int thumbs_up;

        @JsonProperty(value = "sound_urls")
        private List<String> sound_urls;

        @JsonProperty(value = "author")
        private String author;
    }

    private static class UrbanDictionarySearchResults {

        private List<SearchResult> searchResults;
    }
}
