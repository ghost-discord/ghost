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
import org.pmw.tinylog.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.util.Optional;

public final class ModuleWatch2gether extends Module {
    private static final String REPLY_ERROR = "Error trying to create a Watch2Gether room.";
    private static final String REPLY_UNCONFIGURED = "The Watch2Gether module has not been configured. Notify the bot owner.";
    private static final String BASE_URL = "https://w2g.tv/rooms/";
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
        if (References.getConfig().w2gApiKey() == null) {
            ctx.replyBlocking(REPLY_UNCONFIGURED);
            return;
        }

        try {
            final Watch2getherResponse room = restTemplate.postForObject(BASE_URL + "create.json",
                    new HttpEntity<>(new ModuleWatch2gether.Watch2getherRequest()),
                    ModuleWatch2gether.Watch2getherResponse.class);

            ctx.replyBlocking(Optional.ofNullable(room)
                    .map(Watch2getherResponse::getUrl)
                    .orElse(REPLY_ERROR));
        } catch (HttpStatusCodeException exception) {
            ctx.replyBlocking(REPLY_ERROR + "(Server responded with code " + exception.getStatusCode().toString() + ")");
            Logger.error(exception, REPLY_ERROR);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Watch2getherRequest {
        @JsonProperty("share")
        private String url = "";

        @JsonProperty("w2g_api_key")
        private String apiKey = References.getConfig().w2gApiKey();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Watch2getherResponse {
        @JsonProperty("streamkey")
        private String id;

        String getUrl() {
            return BASE_URL + id;
        }
    }
}
