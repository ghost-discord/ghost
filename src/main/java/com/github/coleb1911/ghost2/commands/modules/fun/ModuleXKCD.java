package com.github.coleb1911.ghost2.commands.modules.fun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.utility.RestUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.util.Random;

public final class ModuleXKCD extends Module {
    private static final String BASE_URL = "https://xkcd.com/";
    private static final String REPLY_FETCH_ERROR = "Error trying to retrieve xkcd.";

    private static final RestTemplate TEMPLATE = RestUtils.defaultRestTemplate();
    private static final Random random = new Random();

    @ReflectiveAccess
    public ModuleXKCD() {
        super(new ModuleInfo.Builder(ModuleXKCD.class)
                .withName("xkcd")
                .withDescription("Fetch an XKCD comic"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        final String url;
        if (ctx.getArgs().isEmpty()) {
            final XKCDComic latest = TEMPLATE.getForObject(BASE_URL + "info.0.json", XKCDComic.class);
            if (latest == null) {
                ctx.replyBlocking(REPLY_FETCH_ERROR);
                return;
            }

            url = BASE_URL + random.nextInt(latest.getNum());
        } else {
            url = BASE_URL + ctx.getArgs().get(0);
        }

        try {
            final XKCDComic comic = TEMPLATE.getForObject(url + "/info.0.json", XKCDComic.class);
            if (comic == null) {
                ctx.replyBlocking(REPLY_FETCH_ERROR);
                return;
            }

            ctx.getChannel().createEmbed(spec -> spec
                    .setTitle("xkcd")
                    .setDescription(comic.getNum() + " - " + comic.getTitle())
                    .setFooter(comic.getAlt(), null)
                    .setImage(comic.getImg())
                    .setUrl(BASE_URL + comic.getNum())
            ).subscribe();
        } catch (HttpStatusCodeException exception) {
            ctx.replyBlocking(REPLY_FETCH_ERROR);
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