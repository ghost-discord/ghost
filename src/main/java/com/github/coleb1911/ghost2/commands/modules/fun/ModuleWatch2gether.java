package com.github.coleb1911.ghost2.commands.modules.fun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.entity.Member;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.util.Base64;

public final class ModuleWatch2gether extends Module {
    private static final String W2G_URL = "https://www.watch2gether.com/rooms/create.json";
    private static final RestTemplate restTemplate = createRestTemplate();

    private static RestTemplate createRestTemplate() {
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).build();
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(client);
        return new RestTemplate(clientHttpRequestFactory);
    }

    @ReflectiveAccess
    public ModuleWatch2gether() {
        super(new ModuleInfo.Builder(ModuleWatch2gether.class)
                .withName("watch2gether")
                .withDescription("Create a Watch2gether room"));
    }

    @Override
    public void invoke(@NotNull CommandContext ctx) {

        try {
            String url;
            if(ctx.getArgs().isEmpty())url ="";
            else url = ctx.getArgs().get(0);
            final ModuleWatch2gether.Watch2getherResponse room = restTemplate.postForObject(W2G_URL,new HttpEntity<Watch2getherRequest>(new ModuleWatch2gether.Watch2getherRequest()), ModuleWatch2gether.Watch2getherResponse.class);
            if (room == null) {
                ctx.reply("Error trying to create a Watch2gether room");
                return;
            }
            final Member me = ctx.getSelf();

            ctx.getChannel().createEmbed(spec -> spec
                    .setAuthor(me.getUsername(), "https://github.com/cbryant02/ghost2", me.getAvatarUrl())
                    .setTitle("New watch2gether room: "+room.getUrl())
                    .setFooter(url, null)
            ).subscribe();
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                ctx.reply("URL Not found");
            } else {
                ctx.reply("Error trying to retrieve the Watch2gether room");
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Watch2getherRequest {
        @JsonProperty("share")
        private String url="";
        @JsonProperty("api_key")
        private String apiKey = new String(Base64.getDecoder().decode("dWp3dzIzNDIzMmV3ZWd3Z3dlZjRk"));


        String getUrl() {
            return url;
        }

        String getApiKey() {
            return apiKey;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Watch2getherResponse {
        private static final String URL = "https://www.watch2gether.com/rooms/";
        @JsonProperty("streamkey")
        private String id;

        String getId() {
            return id;
        }

        String getUrl(){
            return URL+id;
        }
    }
}
