package com.github.coleb1911.ghost2.commands.modules.fun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.entity.Member;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.util.Random;

public final class ModuleXKCD extends Module {
    private static final RestTemplate restTemplate = createRestTemplate();
    private static final Random random = new Random();

    private static RestTemplate createRestTemplate() {
        HttpClient client = HttpClientBuilder.create().build();
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(client);
        return new RestTemplate(clientHttpRequestFactory);
    }

    @ReflectiveAccess
    public ModuleXKCD() {
        super(new ModuleInfo.Builder(ModuleXKCD.class)
                .withName("xkcd")
                .withDescription("Show XKCD"));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {
        final String url;
        if (ctx.getArgs().isEmpty()) {
            final XKCDComic latest = restTemplate.getForObject("https://xkcd.com/info.0.json", XKCDComic.class);
            url = "https://xkcd.com/" + random.nextInt(latest.getNum());
        } else {
            url = "https://xkcd.com/" + ctx.getArgs().get(0);
        }
        try {
            final XKCDComic comic = restTemplate.getForObject(url + "/info.0.json", XKCDComic.class);
            if (comic == null) {
                ctx.reply("Error trying to retrieve xkcd");
                return;
            }
            final Member me = ctx.getSelf();

            ctx.getChannel().createEmbed(spec -> spec
                    .setAuthor(me.getUsername(), "https://github.com/cbryant02/ghost2", me.getAvatarUrl())
                    .setTitle(comic.getNum() + " - " + comic.getTitle())
                    .setFooter(comic.getAlt(), null)
                    .setImage(comic.getImg())
            ).subscribe();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                ctx.reply("XKCD comic not found");
            } else {
                ctx.reply("Error trying to retrieve xkcd");
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class XKCDComic {
        @JsonProperty("alt")
        private String alt;
        @JsonProperty("img")
        private String img;
        @JsonProperty("title")
        private String title;
        @JsonProperty("num")
        private int num;

        String getAlt() {
            return alt;
        }

        String getImg() {
            return img;
        }

        String getTitle() {
            return title;
        }

        int getNum() {
            return num;
        }
    }
}