package com.github.coleb1911.ghost2.commands.modules.info;

import com.github.coleb1911.ghost2.References;
import com.github.coleb1911.ghost2.commands.meta.CommandContext;
import com.github.coleb1911.ghost2.commands.meta.Module;
import com.github.coleb1911.ghost2.commands.meta.ModuleInfo;
import com.github.coleb1911.ghost2.commands.meta.ReflectiveAccess;
import discord4j.core.object.entity.Member;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;

public final class ModuleAbout extends Module {
    private static final String GITHUB_URL = "https://github.com/cbryant02/ghost2";
    private static final String FLAVOR_TEXT = "Want to help contribute? Visit our GitHub: " + GITHUB_URL + "\n" +
            "You can also report bugs there by opening an issue, or by messaging @yaboired#8927.";
    private static final String FIELD_ID = "\uD83C\uDD94 My ID";
    private static final String FIELD_TAG = "\u0023\u20E3 My tag";
    private static final String FIELD_SERVER_TIME = "\u23F1 Time in this server";
    private static final String FOOTER = "ghost2 v" + References.VERSION_STRING;

    @ReflectiveAccess
    public ModuleAbout() {
        super(new ModuleInfo.Builder(ModuleAbout.class)
                .withName("about")
                .withDescription("Information about the bot"));
    }

    @Override
    @ReflectiveAccess
    public void invoke(@NotNull CommandContext ctx) {
        Member me = ctx.getSelf();

        Duration timeInServer = Duration.between(me.getJoinTime(), Instant.now());
        String timeFormatted = DurationFormatUtils.formatDuration(timeInServer.toMillis(), "dd 'days', HH 'hours', mm 'minutes'");

        ctx.getChannel().createEmbed(embedCreateSpec -> embedCreateSpec
                .setAuthor(me.getUsername(), GITHUB_URL, me.getAvatarUrl())
                .setTitle("About")
                .setDescription(FLAVOR_TEXT)
                .addField(FIELD_ID, me.getId().asString(), false)
                .addField(FIELD_TAG, me.getUsername() + "#" + me.getDiscriminator(), false)
                .addField(FIELD_SERVER_TIME, timeFormatted, false)
                .setFooter(FOOTER, null)).subscribe();
    }
}