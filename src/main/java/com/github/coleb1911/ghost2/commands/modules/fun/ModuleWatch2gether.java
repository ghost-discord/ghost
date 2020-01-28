package com.github.coleb1911.ghost2.commands.modules.fun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.coleb1911.ghost2.References;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import com.github.coleb1911.ghost2.utility.RestUtils;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;

public final class ModuleWatch2gether extends Module {
    private static final String W2G_URL = "https://www.watch2gether.com/rooms/create.json";
    private static final RestTemplate restTemplate = RestUtils.defaultRestTemplate();

    @ReflectiveAccess
    public ModuleWatch2gether() {
        super(new ModuleInfo.Builder(ModuleWatch2gether.class)
                .withName("watch2gether")
                .withDescription("Create a Watch2Gether room")
                .withAliases("w2g")
                .withBotPermissions(PermissionSet.of(Permission.EMBED_LINKS)));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        try {
            final Watch2getherResponse room = restTemplate.postForObject(W2G_URL, new HttpEntity<>(new ModuleWatch2gether.Watch2getherRequest()), ModuleWatch2gether.Watch2getherResponse.class);
            if (room == null) {
                ctx.replyBlocking("Error trying to create a Watch2Gether room.");
                return;
            }

            ctx.replyBlocking(room.getUrl());
        } catch (HttpStatusCodeException exception) {
            ctx.replyBlocking("Error trying to retrieve the Watch2Gether room.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Watch2getherRequest {
        @JsonProperty("share")
        private String url = "";

        @JsonProperty("api_key")
        private String apiKey = References.getConfig().w2gApiKey();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Watch2getherResponse {
        private static final String URL = "https://www.watch2gether.com/rooms/";

        @JsonProperty("streamkey")
        private String id;

        String getUrl() {
            return URL + id;
        }
    }
}
